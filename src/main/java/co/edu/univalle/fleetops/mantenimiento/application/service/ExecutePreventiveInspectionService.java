package co.edu.univalle.fleetops.mantenimiento.application.service;

import co.edu.univalle.fleetops.mantenimiento.domain.event.PreventiveMaintenanceRequiredEvent;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.OutboxMessage;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.ExecutePreventiveInspectionUseCase;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OrderRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OutboxRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.RuleRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort.VehicleSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * HU-02: Ejecución de la inspección preventiva periódica multicriterio.
 *
 * <p>No se anota con {@code @Transactional} a nivel de clase para evitar transacciones
 * abiertas durante llamadas remotas a MS Vehículos (DAS y directrices de arquitectura).
 */
@Service
public class ExecutePreventiveInspectionService implements ExecutePreventiveInspectionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutePreventiveInspectionService.class);
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    private final VehicleServiceClientPort vehicleClient;
    private final RuleRepositoryPort ruleRepository;
    private final OrderRepositoryPort orderRepository;
    private final EventPublisherPort eventPublisher;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;
    private final Counter generatedOrdersCounter;

    public ExecutePreventiveInspectionService(
            VehicleServiceClientPort vehicleClient,
            RuleRepositoryPort ruleRepository,
            OrderRepositoryPort orderRepository,
            EventPublisherPort eventPublisher,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.vehicleClient = vehicleClient;
        this.ruleRepository = ruleRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.generatedOrdersCounter = meterRegistry.counter("scheduler_mantenimientos_generados_total");
    }

    @Override
    public void executeInspection() {
        runInspectionProcess();
    }

    private void runInspectionProcess() {
        LOG.info("Iniciando ciclo de inspección preventiva periódica");

        List<VehicleSummary> activeVehicles;
        try {
            activeVehicles = vehicleClient.getActiveVehicles();
        } catch (Exception ex) {
            LOG.error("Fallo crítico: No fue posible consultar la flota activa en MS Vehículos", ex);
            throw new ServiceUnavailableException("VEHICLE_FLEET_UNAVAILABLE",
                    "No fue posible consultar la flota activa en MS Vehículos");
        }

        if (activeVehicles == null || activeVehicles.isEmpty()) {
            LOG.info("Ejecución finalizada: No hay vehículos activos para evaluar");
            return;
        }

        for (VehicleSummary vehicle : activeVehicles) {
            try {
                evaluateVehicle(vehicle);
            } catch (Exception ex) {
                LOG.error("Error no controlado al procesar vehículo {}: {}", vehicle.id(), ex.getMessage(), ex);
            }
        }

        LOG.info("Ciclo de inspección preventiva periódica completado exitosamente");
    }

    private void evaluateVehicle(VehicleSummary vehicle) {
        if (orderRepository.findActiveOrderByVehicleId(vehicle.id()).isPresent()) {
            LOG.info("Vehículo {} ya posee una orden preventiva abierta en curso", vehicle.id());
            return;
        }

        Optional<MaintenanceRule> applicableRuleOpt = ruleRepository.findApplicableRule(
                vehicle.id(),
                vehicle.category()
        );
        if (applicableRuleOpt.isEmpty()) {
            LOG.debug("Vehículo {} no tiene regla preventiva aplicable activa", vehicle.id());
            return;
        }

        MaintenanceRule rule = applicableRuleOpt.get();
        LocalDateTime currentTime = now();
        Optional<EvaluationResult> evaluationOpt = evaluateCriteria(vehicle, rule, currentTime);
        if (evaluationOpt.isEmpty()) {
            return;
        }

        EvaluationResult evaluation = evaluationOpt.get();
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicle.id())
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .origin(MaintenanceOrigin.SCHEDULER)
                .currentMileage(evaluation.currentKm())
                .description(rule.getRoutineDescription())
                .reason(evaluation.reason())
                .createdAt(currentTime)
                .updatedAt(currentTime)
                .build();

        MaintenanceOrder savedOrder = orderRepository.save(order);
        if (!lockVehicleOrCompensate(savedOrder)) {
            return;
        }

        PreventiveMaintenanceRequiredEvent event = PreventiveMaintenanceRequiredEvent.create(
                savedOrder.getId(),
                vehicle.id(),
                evaluation.reason(),
                evaluation.currentKm(),
                evaluation.thresholdKm(),
                evaluation.deltaDays()
        );

        publishEventWithOutboxFallback(savedOrder.getId(), event, currentTime);
        generatedOrdersCounter.increment();
        LOG.info("Orden preventiva {} generada para vehículo {} por motivo {}",
                savedOrder.getId(), vehicle.id(), evaluation.reason());
    }

    private Optional<EvaluationResult> evaluateCriteria(
            VehicleSummary vehicle,
            MaintenanceRule rule,
            LocalDateTime currentTime
    ) {
        Optional<MaintenanceOrder> latestCompletedOpt = orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id());

        int currentKm = Objects.requireNonNullElse(vehicle.mileage(), 0);
        int previousKm = latestCompletedOpt
                .map(o -> Objects.requireNonNullElse(o.getCurrentMileage(), 0))
                .orElse(0);
        int deltaKm = Math.max(0, currentKm - previousKm);

        LocalDateTime baseDate = latestCompletedOpt
                .map(o -> Objects.requireNonNullElse(o.getCompletedAt(), o.getCreatedAt()))
                .orElseGet(() -> Objects.requireNonNullElse(rule.getCreatedAt(), currentTime));

        long deltaDays = Math.max(0, ChronoUnit.DAYS.between(
                baseDate.atZone(BOGOTA_ZONE),
                currentTime.atZone(BOGOTA_ZONE)
        ));

        int thresholdKm = Objects.requireNonNullElse(rule.getMileageIntervalKm(), Integer.MAX_VALUE);
        int thresholdDays = Objects.requireNonNullElse(rule.getTimeIntervalDays(), Integer.MAX_VALUE);

        boolean kmExceeded = deltaKm >= thresholdKm;
        boolean daysExceeded = deltaDays >= thresholdDays;
        if (!kmExceeded && !daysExceeded) {
            return Optional.empty();
        }

        String reason = resolveReason(kmExceeded, daysExceeded);
        return Optional.of(new EvaluationResult(reason, currentKm, thresholdKm, deltaDays));
    }

    private static String resolveReason(boolean kmExceeded, boolean daysExceeded) {
        if (kmExceeded && daysExceeded) {
            return "AMBOS_CRITERIOS_SUPERADOS";
        }
        return kmExceeded ? "KILOMETRAJE_SUPERADO" : "TIEMPO_VENCIDO";
    }

    private boolean lockVehicleOrCompensate(MaintenanceOrder savedOrder) {
        boolean statusUpdated;
        try {
            statusUpdated = vehicleClient.updateVehicleStatus(
                    savedOrder.getVehicleId(),
                    savedOrder.getId(),
                    "IN_MAINTENANCE"
            );
        } catch (Exception ex) {
            LOG.warn("Excepción al intentar bloquear vehículo {}: {}",
                    savedOrder.getVehicleId(), ex.getMessage());
            statusUpdated = false;
        }

        if (!statusUpdated) {
            LOG.error("Error al bloquear vehículo {} en MS Vehículos; orden revertida", savedOrder.getVehicleId());
            savedOrder.cancel("Fallo al bloquear vehículo en MS Vehículos");
            orderRepository.save(savedOrder);
            return false;
        }
        return true;
    }

    private void publishEventWithOutboxFallback(
            UUID orderId,
            PreventiveMaintenanceRequiredEvent event,
            LocalDateTime createdAt
    ) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            LOG.error("Error serializando evento de mantenimiento preventivo para orden {}: {}",
                    orderId, ex.getMessage());
            payloadJson = "{}";
        }

        try {
            eventPublisher.publish(null, PreventiveMaintenanceRequiredEvent.SUBJECT, event);
        } catch (Exception ex) {
            LOG.warn("Fallo al publicar evento asíncrono para orden {}. Persistiendo en Outbox: {}",
                    orderId, ex.getMessage());
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("MAINTENANCE_ORDER")
                    .aggregateId(orderId)
                    .eventType(PreventiveMaintenanceRequiredEvent.EVENT_TYPE)
                    .destination(PreventiveMaintenanceRequiredEvent.SUBJECT)
                    .routingKey(PreventiveMaintenanceRequiredEvent.SUBJECT)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .errorMessage(ex.getMessage())
                    .createdAt(createdAt)
                    .build();
            outboxRepository.save(outboxMessage);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BOGOTA_ZONE);
    }

    private record EvaluationResult(String reason, int currentKm, int thresholdKm, long deltaDays) {
    }
}