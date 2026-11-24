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
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    private final VehicleServiceClientPort vehicleClient;
    private final RuleRepositoryPort ruleRepository;
    private final OrderRepositoryPort orderRepository;
    private final EventPublisherPort eventPublisher;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;
    private final Counter generatedOrdersCounter;
    private final Timer inspectionTimer;

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
        this.inspectionTimer = meterRegistry.timer("scheduler_duracion_segundos");
    }

    @Override
    public void executeInspection() {
        inspectionTimer.record(this::runInspectionProcess);
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
        // 1. Control de Idempotencia: Verificar orden activa (SCHEDULED_PENDING o IN_PROGRESS)
        if (orderRepository.findActiveOrderByVehicleId(vehicle.id()).isPresent()) {
            LOG.info("Vehículo {} ya posee una orden preventiva abierta en curso", vehicle.id());
            return;
        }

        // 2. Resolución Jerárquica de Regla: Específica -> Categoría -> Global
        Optional<MaintenanceRule> applicableRuleOpt = ruleRepository.findApplicableRule(
                vehicle.id(),
                vehicle.category()
        );

        if (applicableRuleOpt.isEmpty()) {
            LOG.debug("Vehículo {} no tiene regla preventiva aplicable activa", vehicle.id());
            return;
        }

        MaintenanceRule rule = applicableRuleOpt.get();

        // 3. Evaluación Multicriterio (Diferencial de km y tiempo)
        Optional<MaintenanceOrder> latestCompletedOpt = orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id());

        int currentKm = vehicle.mileage() != null ? vehicle.mileage() : 0;
        int previousKm = latestCompletedOpt.map(o -> o.getCurrentMileage() != null ? o.getCurrentMileage() : 0).orElse(0);
        int deltaKm = Math.max(0, currentKm - previousKm);

        LocalDateTime baseDate = latestCompletedOpt
                .map(o -> o.getCompletedAt() != null ? o.getCompletedAt() : o.getCreatedAt())
                .orElseGet(() -> rule.getCreatedAt() != null ? rule.getCreatedAt() : LocalDateTime.now());

        long deltaDays = Math.max(0, ChronoUnit.DAYS.between(baseDate, LocalDateTime.now()));

        int thresholdKm = rule.getMileageIntervalKm() != null ? rule.getMileageIntervalKm() : Integer.MAX_VALUE;
        int thresholdDays = rule.getTimeIntervalDays() != null ? rule.getTimeIntervalDays() : Integer.MAX_VALUE;

        boolean kmExceeded = deltaKm >= thresholdKm;
        boolean daysExceeded = deltaDays >= thresholdDays;

        if (!kmExceeded && !daysExceeded) {
            return;
        }

        String reason;
        if (kmExceeded && daysExceeded) {
            reason = "AMBOS_CRITERIOS_SUPERADOS";
        } else if (kmExceeded) {
            reason = "KILOMETRAJE_SUPERADO";
        } else {
            reason = "TIEMPO_VENCIDO";
        }

        // 4. Persistir Orden Preventiva
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicle.id())
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .origin(MaintenanceOrigin.SCHEDULER)
                .currentMileage(currentKm)
                .description(rule.getRoutineDescription())
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        MaintenanceOrder savedOrder = orderRepository.save(order);

        // 5. Bloqueo Síncrono del Vehículo en MS Vehículos con Transacción de Compensación
        boolean statusUpdated;
        try {
            statusUpdated = vehicleClient.updateVehicleStatus(vehicle.id(), "EN_MANTENIMIENTO");
        } catch (Exception ex) {
            LOG.warn("Excepción al intentar bloquear vehículo {}: {}", vehicle.id(), ex.getMessage());
            statusUpdated = false;
        }

        if (!statusUpdated) {
            LOG.error("Error al bloquear vehículo {} en MS Vehículos; orden revertida", vehicle.id());
            savedOrder.cancel("Fallo al bloquear vehículo en MS Vehículos");
            orderRepository.save(savedOrder);
            return;
        }

        // 6. Publicación Asíncrona del Evento con Resiliencia (Transactional Outbox Fallback)
        PreventiveMaintenanceRequiredEvent event = PreventiveMaintenanceRequiredEvent.create(
                savedOrder.getId(),
                vehicle.id(),
                reason,
                currentKm,
                thresholdKm,
                deltaDays
        );

        publishEventWithOutboxFallback(savedOrder.getId(), event);

        // 7. Métrica de Prometheus
        generatedOrdersCounter.increment();
        LOG.info("Orden preventiva {} generada para vehículo {} por motivo {}", savedOrder.getId(), vehicle.id(), reason);
    }

    private void publishEventWithOutboxFallback(UUID orderId, PreventiveMaintenanceRequiredEvent event) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            LOG.error("Error serializando evento de mantenimiento preventivo para orden {}: {}", orderId, ex.getMessage());
            payloadJson = "{}";
        }

        try {
            eventPublisher.publish(null, PreventiveMaintenanceRequiredEvent.SUBJECT, event);
        } catch (Exception ex) {
            LOG.warn("Fallo al publicar evento asíncrono para orden {}. Persistiendo en Outbox: {}", orderId, ex.getMessage());
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
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outboxMessage);
        }
    }
}
