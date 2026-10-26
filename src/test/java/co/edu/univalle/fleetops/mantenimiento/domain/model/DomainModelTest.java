package co.edu.univalle.fleetops.mantenimiento.domain.model;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.BusinessRuleException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.InvalidStateTransitionException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.LogType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Domain Models Unit Tests")
class DomainModelTest {

    @Test
    @DisplayName("Debe permitir ciclo de vida completo de orden de mantenimiento y calcular costo")
    void shouldAllowValidStateTransitions() {
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .origin(MaintenanceOrigin.SCHEDULER)
                .build();

        UUID technicianId = UUID.randomUUID();
        order.assignTechnician(technicianId);
        assertEquals(technicianId, order.getTechnicianId());

        order.startProgress();
        assertEquals(MaintenanceStatus.IN_PROGRESS, order.getStatus());

        order.complete("TECHNICIAN", technicianId.toString(), BigDecimal.valueOf(150000));
        assertEquals(MaintenanceStatus.COMPLETED, order.getStatus());
        assertEquals(BigDecimal.valueOf(150000), order.getTotalCost());
        assertNotNull(order.getCompletedAt());
        assertNotNull(order.getUpdatedAt());
    }

    @Test
    @DisplayName("Debe contemplar transiciones inválidas en orden")
    void shouldEnforceInvalidStateTransitions() {
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .build();

        // Completar sin iniciar
        assertThrows(InvalidStateTransitionException.class, () ->
                order.complete("TECHNICIAN", "tec-1", BigDecimal.ZERO)
        );

        // Cancelar orden pendiente
        order.cancel("Vehículo dado de baja");
        assertEquals(MaintenanceStatus.CANCELLED, order.getStatus());
        assertEquals("Vehículo dado de baja", order.getReason());

        // Asignar técnico a orden cancelada
        assertThrows(InvalidStateTransitionException.class, () ->
                order.assignTechnician(UUID.randomUUID())
        );

        // Iniciar orden cancelada
        assertThrows(InvalidStateTransitionException.class, order::startProgress);

        // Crear orden completada
        MaintenanceOrder completedOrder = MaintenanceOrder.builder()
                .id(UUID.randomUUID())
                .status(MaintenanceStatus.COMPLETED)
                .build();

        // Asignar técnico o cancelar orden completada
        assertThrows(InvalidStateTransitionException.class, () ->
                completedOrder.assignTechnician(UUID.randomUUID())
        );
        assertThrows(InvalidStateTransitionException.class, () ->
                completedOrder.cancel("Intento cancelación")
        );

        // Completar con costo nulo asigna BigDecimal.ZERO
        MaintenanceOrder inProgress = MaintenanceOrder.builder()
                .status(MaintenanceStatus.IN_PROGRESS)
                .build();
        inProgress.complete("SUPERVISOR", "sup-1", null);
        assertEquals(BigDecimal.ZERO, inProgress.getTotalCost());
    }

    @Test
    @DisplayName("Debe validar umbrales y alcances en reglas de mantenimiento preventivo")
    void shouldValidateMaintenanceRuleThresholds() {
        // Scope nulo
        MaintenanceRule nullScope = MaintenanceRule.builder().build();
        assertThrows(BusinessRuleException.class, nullScope::validate);

        // Scope CATEGORY sin categoría
        MaintenanceRule catWithoutCat = MaintenanceRule.builder()
                .scope(RuleScope.CATEGORY)
                .mileageIntervalKm(10000)
                .timeIntervalDays(90)
                .build();
        assertThrows(BusinessRuleException.class, catWithoutCat::validate);

        // Scope VEHICLE sin vehicleId
        MaintenanceRule vehWithoutId = MaintenanceRule.builder()
                .scope(RuleScope.VEHICLE)
                .mileageIntervalKm(10000)
                .timeIntervalDays(90)
                .build();
        assertThrows(BusinessRuleException.class, vehWithoutId::validate);

        // Kilometraje fuera de rango
        MaintenanceRule lowKm = MaintenanceRule.builder()
                .scope(RuleScope.GLOBAL)
                .mileageIntervalKm(500)
                .timeIntervalDays(90)
                .build();
        assertThrows(BusinessRuleException.class, lowKm::validate);

        MaintenanceRule highKm = MaintenanceRule.builder()
                .scope(RuleScope.GLOBAL)
                .mileageIntervalKm(60000)
                .timeIntervalDays(90)
                .build();
        assertThrows(BusinessRuleException.class, highKm::validate);

        // Días fuera de rango
        MaintenanceRule lowDays = MaintenanceRule.builder()
                .scope(RuleScope.GLOBAL)
                .mileageIntervalKm(10000)
                .timeIntervalDays(15)
                .build();
        assertThrows(BusinessRuleException.class, lowDays::validate);

        MaintenanceRule highDays = MaintenanceRule.builder()
                .scope(RuleScope.GLOBAL)
                .mileageIntervalKm(10000)
                .timeIntervalDays(400)
                .build();
        assertThrows(BusinessRuleException.class, highDays::validate);

        // Regla válida
        MaintenanceRule validRule = MaintenanceRule.builder()
                .scope(RuleScope.CATEGORY)
                .category(VehicleCategory.CAMIONETA)
                .mileageIntervalKm(10000)
                .timeIntervalDays(180)
                .build();
        assertDoesNotThrow(validRule::validate);

        // Getters / Setters auxiliares
        validRule.setThresholdKm(12000);
        validRule.setThresholdDays(150);
        assertEquals(12000, validRule.getThresholdKm());
        assertEquals(150, validRule.getThresholdDays());
    }

    @Test
    @DisplayName("Debe verificar getters, setters y constructores de modelos auxiliares")
    void shouldCoverAuxiliaryDomainModels() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // AccessAuditLog
        AccessAuditLog audit = new AccessAuditLog();
        audit.setId(id);
        audit.setActorType("USER");
        audit.setActorId(id.toString());
        audit.setActorEmail("admin@fleetops.com");
        audit.setAction("VIEW");
        audit.setResourceAccessed("/api/v1/orders");
        audit.setHttpMethod("GET");
        audit.setIpAddress("127.0.0.1");
        audit.setUserAgent("curl");
        audit.setStatusResult("200");
        audit.setDetails("ok");
        audit.setCreatedAt(now);

        assertEquals(id, audit.getId());
        assertEquals("USER", audit.getActorType());
        assertEquals("admin@fleetops.com", audit.getActorEmail());

        // MaintenanceLog & Attachment
        MaintenanceLog log = new MaintenanceLog();
        log.setId(id);
        log.setOrderId(id);
        log.setLogType(LogType.REVISION);
        log.setNotes("Todo correcto");
        log.setCostDelta(BigDecimal.TEN);
        log.setAuthorType("TECH");
        log.setAuthorId(id.toString());
        log.setAuthorName("Carlos");
        log.setRegisteredAt(now);
        log.setAttachments(new ArrayList<>());

        assertEquals(id, log.getId());
        assertEquals(LogType.REVISION, log.getLogType());

        MaintenanceLogAttachment attachment = new MaintenanceLogAttachment();
        attachment.setId(id);
        attachment.setMaintenanceLogId(id);
        attachment.setFileUrl("http://img");
        attachment.setFileName("img.png");
        attachment.setMimeType("image/png");
        attachment.setFileSizeBytes(500L);
        attachment.setCreatedAt(now);

        assertEquals("img.png", attachment.getFileName());

        // OutboxMessage
        OutboxMessage outbox = new OutboxMessage();
        outbox.setId(id);
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId(id);
        outbox.setEventType("ORDER_CREATED");
        outbox.setExchange("exchange");
        outbox.setRoutingKey("routing.key");
        outbox.setDestination("dest");
        outbox.setPayload("{}");
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setErrorMessage(null);
        outbox.setCreatedAt(now);
        outbox.setProcessedAt(null);

        assertEquals("ORDER_CREATED", outbox.getEventType());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());

        // Technician
        Technician tech = new Technician();
        tech.setId(id);
        tech.setName("Laura");
        tech.setEmail("laura@fleetops.com");
        tech.setPhoneNumber("+573001234567");
        tech.setIsActive(true);
        tech.setCreatedAt(now);
        tech.setUpdatedAt(now);

        assertEquals("Laura", tech.getName());
        assertEquals("laura@fleetops.com", tech.getEmail());
        assertEquals("+573001234567", tech.getPhoneNumber());
    }
}
