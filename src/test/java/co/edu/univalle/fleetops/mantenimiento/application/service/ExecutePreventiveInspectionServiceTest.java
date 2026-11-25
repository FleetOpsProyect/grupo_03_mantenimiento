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
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OrderRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OutboxRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.RuleRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort.VehicleSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutePreventiveInspectionServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, Month.NOVEMBER, 24, 2, 0);

    @Mock
    private VehicleServiceClientPort vehicleClient;
    @Mock
    private RuleRepositoryPort ruleRepository;
    @Mock
    private OrderRepositoryPort orderRepository;
    @Mock
    private EventPublisherPort eventPublisher;
    @Mock
    private OutboxRepositoryPort outboxRepository;

    private SimpleMeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private ExecutePreventiveInspectionService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        service = new ExecutePreventiveInspectionService(
                vehicleClient,
                ruleRepository,
                orderRepository,
                eventPublisher,
                outboxRepository,
                objectMapper,
                meterRegistry
        );
    }

    private VehicleSummary createVehicle(int mileage, VehicleCategory category) {
        return new VehicleSummary(UUID.randomUUID(), mileage, "ACTIVO", category);
    }

    private MaintenanceRule createRule(int kmThreshold, int daysThreshold) {
        return MaintenanceRule.builder()
                .id(UUID.randomUUID())
                .mileageIntervalKm(kmThreshold)
                .timeIntervalDays(daysThreshold)
                .routineDescription("Rutina general preventiva")
                .createdAt(FIXED_NOW.minusDays(10))
                .build();
    }

    @Test
    @DisplayName("Criterio 1: Flota activa vacía culmina sin fallos ni llamadas adicionales")
    void shouldCompleteWithoutFailureWhenActiveVehiclesListIsEmpty() {
        when(vehicleClient.getActiveVehicles()).thenReturn(List.of());

        service.executeInspection();

        verifyNoInteractions(ruleRepository, orderRepository, eventPublisher, outboxRepository);
        assertEquals(0.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterio 1: Fallo crítico en MS Vehículos registra error y lanza ServiceUnavailableException")
    void shouldThrowServiceUnavailableWhenVehicleClientFails() {
        when(vehicleClient.getActiveVehicles()).thenThrow(new RuntimeException("MS Vehículos connection timeout"));

        assertThrows(ServiceUnavailableException.class, () -> service.executeInspection());

        verifyNoInteractions(ruleRepository, orderRepository, eventPublisher, outboxRepository);
    }

    @Test
    @DisplayName("Criterio 3: Omite vehículo si ya posee una orden activa en SCHEDULED_PENDING o IN_PROGRESS")
    void shouldSkipVehicleWhenActiveOrderAlreadyExists() {
        VehicleSummary vehicle = createVehicle(15000, VehicleCategory.AUTOMOVIL);
        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id()))
                .thenReturn(Optional.of(MaintenanceOrder.builder().status(MaintenanceStatus.SCHEDULED_PENDING).build()));

        service.executeInspection();

        verify(ruleRepository, never()).findApplicableRule(any(), any());
        verify(orderRepository, never()).save(any());
        assertEquals(0.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterio 2: Omite vehículo si no existe ninguna regla preventiva aplicable")
    void shouldSkipVehicleWhenNoApplicableRuleExists() {
        VehicleSummary vehicle = createVehicle(15000, VehicleCategory.CAMIONETA);
        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.CAMIONETA)).thenReturn(Optional.empty());

        service.executeInspection();

        verify(orderRepository, never()).save(any());
        assertEquals(0.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterio 2: No califica si no se supera ni umbral de km ni umbral de días")
    void shouldNotGenerateOrderWhenThresholdsNotMet() {
        VehicleSummary vehicle = createVehicle(4000, VehicleCategory.CAMIONETA);
        MaintenanceRule rule = createRule(10000, 90);

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.CAMIONETA)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());

        service.executeInspection();

        verify(orderRepository, never()).save(any());
        assertEquals(0.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterios 2, 3, 4, 5: Genera orden preventiva por KILOMETRAJE_SUPERADO, bloquea vehículo y publica evento")
    void shouldCreateOrderWhenMileageThresholdExceeded() {
        VehicleSummary vehicle = createVehicle(18000, VehicleCategory.AUTOMOVIL);
        MaintenanceRule rule = createRule(10000, 180);
        MaintenanceOrder previousOrder = MaintenanceOrder.builder()
                .currentMileage(5000)
                .completedAt(FIXED_NOW.minusDays(30))
                .build();

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.AUTOMOVIL)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.of(previousOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleClient.updateVehicleStatus(eq(vehicle.id()), any(), any())).thenReturn(true);

        service.executeInspection();

        ArgumentCaptor<MaintenanceOrder> orderCaptor = ArgumentCaptor.forClass(MaintenanceOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        MaintenanceOrder saved = orderCaptor.getValue();

        assertEquals(vehicle.id(), saved.getVehicleId());
        assertEquals(MaintenanceType.PREVENTIVE, saved.getMaintenanceType());
        assertEquals(MaintenanceStatus.SCHEDULED_PENDING, saved.getStatus());
        assertEquals(MaintenanceOrigin.SCHEDULER, saved.getOrigin());
        assertEquals("KILOMETRAJE_SUPERADO", saved.getReason());
        assertEquals(rule.getRoutineDescription(), saved.getDescription());

        verify(vehicleClient).updateVehicleStatus(eq(vehicle.id()), any(UUID.class), eq("IN_MAINTENANCE"));
        verify(eventPublisher).publish(
                eq(null),
                eq(PreventiveMaintenanceRequiredEvent.SUBJECT),
                any(PreventiveMaintenanceRequiredEvent.class)
        );
        verifyNoInteractions(outboxRepository);
        assertEquals(1.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterio 2: Genera orden preventiva por TIEMPO_VENCIDO cuando se supera el umbral de días")
    void shouldCreateOrderWhenDaysThresholdExceeded() {
        VehicleSummary vehicle = createVehicle(6000, VehicleCategory.CARGA_PESADA);
        MaintenanceRule rule = createRule(10000, 60);
        MaintenanceOrder previousOrder = MaintenanceOrder.builder()
                .currentMileage(5000)
                .completedAt(FIXED_NOW.minusDays(70))
                .build();

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.CARGA_PESADA)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.of(previousOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleClient.updateVehicleStatus(eq(vehicle.id()), any(), any())).thenReturn(true);

        service.executeInspection();

        ArgumentCaptor<MaintenanceOrder> orderCaptor = ArgumentCaptor.forClass(MaintenanceOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals("TIEMPO_VENCIDO", orderCaptor.getValue().getReason());
        assertEquals(1.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterio 2: Genera orden preventiva con motivo AMBOS_CRITERIOS_SUPERADOS")
    void shouldCreateOrderWhenBothThresholdsExceeded() {
        VehicleSummary vehicle = createVehicle(20000, VehicleCategory.CAMIONETA);
        MaintenanceRule rule = createRule(5000, 30);
        MaintenanceOrder previousOrder = MaintenanceOrder.builder()
                .currentMileage(10000)
                .completedAt(FIXED_NOW.minusDays(45))
                .build();

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.CAMIONETA)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.of(previousOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleClient.updateVehicleStatus(eq(vehicle.id()), any(), any())).thenReturn(true);

        service.executeInspection();

        ArgumentCaptor<MaintenanceOrder> orderCaptor = ArgumentCaptor.forClass(MaintenanceOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals("AMBOS_CRITERIOS_SUPERADOS", orderCaptor.getValue().getReason());
    }

    @Test
    @DisplayName("Criterio 4: Transacción de compensación revierte orden a CANCELLED si falla bloqueo en MS Vehículos")
    void shouldCompensateOrderWhenVehicleStatusUpdateFails() {
        VehicleSummary vehicle = createVehicle(15000, VehicleCategory.AUTOMOVIL);
        MaintenanceRule rule = createRule(10000, 90);

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.AUTOMOVIL)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleClient.updateVehicleStatus(eq(vehicle.id()), any(), any())).thenReturn(false);

        service.executeInspection();

        ArgumentCaptor<MaintenanceOrder> orderCaptor = ArgumentCaptor.forClass(MaintenanceOrder.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        MaintenanceOrder compensatedOrder = orderCaptor.getAllValues().get(1);

        assertEquals(MaintenanceStatus.CANCELLED, compensatedOrder.getStatus());
        verifyNoInteractions(eventPublisher, outboxRepository);
        assertEquals(0.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }

    @Test
    @DisplayName("Criterio 4: Transacción de compensación ante excepción remota al actualizar estado")
    void shouldCompensateOrderWhenVehicleStatusUpdateThrowsException() {
        VehicleSummary vehicle = createVehicle(15000, VehicleCategory.AUTOMOVIL);
        MaintenanceRule rule = createRule(10000, 90);

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.AUTOMOVIL)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleClient.updateVehicleStatus(eq(vehicle.id()), any(), any()))
                .thenThrow(new RuntimeException("Network down"));

        service.executeInspection();

        ArgumentCaptor<MaintenanceOrder> orderCaptor = ArgumentCaptor.forClass(MaintenanceOrder.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        MaintenanceOrder compensatedOrder = orderCaptor.getAllValues().get(1);

        assertEquals(MaintenanceStatus.CANCELLED, compensatedOrder.getStatus());
        verifyNoInteractions(eventPublisher, outboxRepository);
    }

    @Test
    @DisplayName("Criterio 5: Persiste en Transactional Outbox si falla la publicación asíncrona del evento")
    void shouldPersistInOutboxWhenEventPublishingFails() {
        VehicleSummary vehicle = createVehicle(15000, VehicleCategory.AUTOMOVIL);
        MaintenanceRule rule = createRule(10000, 90);

        when(vehicleClient.getActiveVehicles()).thenReturn(List.of(vehicle));
        when(orderRepository.findActiveOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(ruleRepository.findApplicableRule(vehicle.id(), VehicleCategory.AUTOMOVIL)).thenReturn(Optional.of(rule));
        when(orderRepository.findLatestCompletedOrderByVehicleId(vehicle.id())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleClient.updateVehicleStatus(eq(vehicle.id()), any(), any())).thenReturn(true);
        doThrow(new RuntimeException("SNS connection timeout")).when(eventPublisher).publish(any(), any(), any());

        service.executeInspection();

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxMessage outbox = outboxCaptor.getValue();

        assertNotNull(outbox);
        assertEquals("MAINTENANCE_ORDER", outbox.getAggregateType());
        assertEquals(PreventiveMaintenanceRequiredEvent.EVENT_TYPE, outbox.getEventType());
        assertEquals(PreventiveMaintenanceRequiredEvent.SUBJECT, outbox.getDestination());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        assertEquals("SNS connection timeout", outbox.getErrorMessage());
        assertEquals(1.0, meterRegistry.counter("scheduler_mantenimientos_generados_total").count());
    }
}