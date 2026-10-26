package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.AccessAuditLog;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLogAttachment;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.OutboxMessage;
import co.edu.univalle.fleetops.mantenimiento.domain.model.Technician;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.LogType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.AccessAuditLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogAttachmentJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.OutboxMessageJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.PreventiveRuleJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Persistence Mappers Unit Tests")
class PersistenceMappersTest {

    private final TechnicianPersistenceMapper technicianMapper = new TechnicianPersistenceMapper();
    private final PreventiveRulePersistenceMapper ruleMapper = new PreventiveRulePersistenceMapper();
    private final MaintenanceLogPersistenceMapper logMapper = new MaintenanceLogPersistenceMapper();
    private final MaintenanceOrderPersistenceMapper orderMapper = new MaintenanceOrderPersistenceMapper(logMapper);
    private final OutboxMessagePersistenceMapper outboxMapper = new OutboxMessagePersistenceMapper();
    private final AccessAuditLogPersistenceMapper auditMapper = new AccessAuditLogPersistenceMapper();

    @Test
    @DisplayName("Technician mapper handles nulls and full entity-domain conversions")
    void testTechnicianMapper() {
        assertNull(technicianMapper.toDomain(null));
        assertNull(technicianMapper.toEntity(null));

        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Technician domain = Technician.builder()
                .id(id)
                .name("Carlos Gomez")
                .email("carlos@example.com")
                .phoneNumber("+573001234567")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        TechnicianJpaEntity entity = technicianMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getName(), entity.getName());
        assertEquals(domain.getEmail(), entity.getEmail());
        assertEquals(domain.getPhoneNumber(), entity.getPhoneNumber());
        assertEquals(domain.getIsActive(), entity.getIsActive());

        Technician mappedBack = technicianMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(domain.getId(), mappedBack.getId());
        assertEquals(domain.getName(), mappedBack.getName());
        assertEquals(domain.getEmail(), mappedBack.getEmail());
        assertEquals(domain.getPhoneNumber(), mappedBack.getPhoneNumber());
    }

    @Test
    @DisplayName("PreventiveRule mapper handles nulls, enums and conversions")
    void testRuleMapper() {
        assertNull(ruleMapper.toDomain(null));
        assertNull(ruleMapper.toEntity(null));

        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MaintenanceRule domain = MaintenanceRule.builder()
                .id(id)
                .scope(RuleScope.CATEGORY)
                .category(VehicleCategory.CARGA_PESADA)
                .mileageIntervalKm(15000)
                .timeIntervalDays(90)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PreventiveRuleJpaEntity entity = ruleMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals("CATEGORY", entity.getScopeType());
        assertEquals("CARGA_PESADA", entity.getVehicleCategory());
        assertEquals(15000, entity.getMileageIntervalKm());
        assertEquals(90, entity.getTimeIntervalDays());

        MaintenanceRule mappedBack = ruleMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(RuleScope.CATEGORY, mappedBack.getScope());
        assertEquals(VehicleCategory.CARGA_PESADA, mappedBack.getCategory());
        assertEquals(15000, mappedBack.getMileageIntervalKm());

        // Domain with null enums
        MaintenanceRule emptyRule = MaintenanceRule.builder().id(id).build();
        PreventiveRuleJpaEntity emptyEntity = ruleMapper.toEntity(emptyRule);
        assertNull(emptyEntity.getScopeType());
        assertNull(emptyEntity.getVehicleCategory());

        // Entity with null strings
        PreventiveRuleJpaEntity rawEntity = PreventiveRuleJpaEntity.builder().id(id).build();
        MaintenanceRule mappedFromRaw = ruleMapper.toDomain(rawEntity);
        assertNull(mappedFromRaw.getScope());
        assertNull(mappedFromRaw.getCategory());
    }

    @Test
    @DisplayName("MaintenanceLog and Attachment mapper handles full structures")
    void testLogAndAttachmentMapper() {
        assertNull(logMapper.toDomain(null));
        assertNull(logMapper.toEntity(null));
        assertNull(logMapper.toAttachmentDomain(null));
        assertNull(logMapper.toAttachmentEntity(null));

        UUID logId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID attachId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MaintenanceLogAttachment attachment = MaintenanceLogAttachment.builder()
                .id(attachId)
                .maintenanceLogId(logId)
                .fileName("foto_frenos.jpg")
                .fileUrl("https://storage.fleetops.com/attachments/foto_frenos.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(102400L)
                .createdAt(now)
                .build();

        MaintenanceLogAttachmentJpaEntity attachEntity = logMapper.toAttachmentEntity(attachment);
        assertNotNull(attachEntity);
        assertEquals("foto_frenos.jpg", attachEntity.getFileName());

        MaintenanceLogAttachment attachDomain = logMapper.toAttachmentDomain(attachEntity);
        assertNotNull(attachDomain);
        assertEquals(attachId, attachDomain.getId());

        MaintenanceLog domain = MaintenanceLog.builder()
                .id(logId)
                .orderId(orderId)
                .logType(LogType.PROGRESS)
                .notes("Reemplazo de pastillas completado")
                .costDelta(new BigDecimal("150.50"))
                .authorType("TECHNICIAN")
                .authorId(UUID.randomUUID().toString())
                .authorName("Juan Perez")
                .registeredAt(now)
                .attachments(List.of(attachment))
                .build();

        MaintenanceLogJpaEntity entity = logMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals("PROGRESS", entity.getLogType());
        assertEquals("Reemplazo de pastillas completado", entity.getNotes());

        entity.setAttachments(List.of(attachEntity));
        MaintenanceLog mappedBack = logMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(LogType.PROGRESS, mappedBack.getLogType());
        assertEquals(1, mappedBack.getAttachments().size());

        // Domain with nulls
        MaintenanceLog nullLog = MaintenanceLog.builder().id(logId).build();
        MaintenanceLogJpaEntity nullLogEntity = logMapper.toEntity(nullLog);
        assertNull(nullLogEntity.getLogType());

        MaintenanceLogJpaEntity rawLogEntity = MaintenanceLogJpaEntity.builder().id(logId).build();
        MaintenanceLog mappedFromRaw = logMapper.toDomain(rawLogEntity);
        assertNull(mappedFromRaw.getLogType());
        assertTrue(mappedFromRaw.getAttachments().isEmpty());
    }

    @Test
    @DisplayName("MaintenanceOrder mapper handles complex graph and nulls")
    void testOrderMapper() {
        assertNull(orderMapper.toDomain(null));
        assertNull(orderMapper.toEntity(null));

        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MaintenanceOrder domain = MaintenanceOrder.builder()
                .id(orderId)
                .vehicleId(vehicleId)
                .technicianId(techId)
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.IN_PROGRESS)
                .origin(MaintenanceOrigin.SCHEDULER)
                .currentMileage(45000)
                .description("Mantenimiento preventivo programado")
                .totalCost(new BigDecimal("320.00"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        MaintenanceOrderJpaEntity entity = orderMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals("PREVENTIVE", entity.getMaintenanceType());
        assertEquals("IN_PROGRESS", entity.getStatus());
        assertEquals("SCHEDULER", entity.getOrigin());

        MaintenanceOrder mappedBack = orderMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(MaintenanceType.PREVENTIVE, mappedBack.getMaintenanceType());
        assertEquals(MaintenanceStatus.IN_PROGRESS, mappedBack.getStatus());
        assertEquals(MaintenanceOrigin.SCHEDULER, mappedBack.getOrigin());

        // Order with null enums
        MaintenanceOrder nullEnums = MaintenanceOrder.builder().id(orderId).build();
        MaintenanceOrderJpaEntity nullEntity = orderMapper.toEntity(nullEnums);
        assertNull(nullEntity.getMaintenanceType());
        assertNull(nullEntity.getStatus());
        assertNull(nullEntity.getOrigin());

        MaintenanceOrderJpaEntity rawEntity = MaintenanceOrderJpaEntity.builder().id(orderId).build();
        MaintenanceOrder mappedFromRaw = orderMapper.toDomain(rawEntity);
        assertNull(mappedFromRaw.getMaintenanceType());
        assertNull(mappedFromRaw.getStatus());
        assertNull(mappedFromRaw.getOrigin());
        assertNotNull(mappedFromRaw.getLogs());
        assertTrue(mappedFromRaw.getLogs().isEmpty());
    }

    @Test
    @DisplayName("OutboxMessage mapper handles status, attempts and dates")
    void testOutboxMapper() {
        assertNull(outboxMapper.toDomain(null));
        assertNull(outboxMapper.toEntity(null));

        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        OutboxMessage domain = OutboxMessage.builder()
                .id(id)
                .aggregateId(aggregateId)
                .eventType("maintenance.order.created")
                .destination("maintenance-exchange")
                .payload("{\"order_id\":\"" + aggregateId + "\"}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .build();

        OutboxMessageJpaEntity entity = outboxMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals("maintenance.order.created", entity.getMessageType());
        assertEquals("maintenance-exchange", entity.getDestination());
        assertEquals("PENDING", entity.getStatus());

        OutboxMessage mappedBack = outboxMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(OutboxStatus.PENDING, mappedBack.getStatus());
        assertEquals("maintenance.order.created", mappedBack.getEventType());

        // Default fallbacks in mapper
        OutboxMessage domainWithoutDestination = OutboxMessage.builder()
                .id(id)
                .routingKey("routing-key-fallback")
                .build();
        OutboxMessageJpaEntity fallbackEntity = outboxMapper.toEntity(domainWithoutDestination);
        assertEquals("EVENT", fallbackEntity.getMessageType());
        assertEquals("routing-key-fallback", fallbackEntity.getDestination());
        assertEquals("PENDING", fallbackEntity.getStatus());
        assertEquals(0, fallbackEntity.getAttempts());

        OutboxMessageJpaEntity rawEntity = OutboxMessageJpaEntity.builder().id(id).build();
        OutboxMessage rawDomain = outboxMapper.toDomain(rawEntity);
        assertNull(rawDomain.getStatus());
    }

    @Test
    @DisplayName("AccessAuditLog mapper handles security audit events")
    void testAuditLogMapper() {
        assertNull(auditMapper.toDomain(null));
        assertNull(auditMapper.toEntity(null));

        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AccessAuditLog domain = AccessAuditLog.builder()
                .id(id)
                .actorType("USER")
                .actorId(actorId.toString())
                .actorEmail("supervisor@fleetops.com")
                .action("ORDER_COMPLETED")
                .resourceAccessed("/api/v1/maintenance/orders/" + id)
                .httpMethod("PATCH")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .statusResult("SUCCESS")
                .details("Order marked as COMPLETED")
                .createdAt(now)
                .build();

        AccessAuditLogJpaEntity entity = auditMapper.toEntity(domain);
        assertNotNull(entity);
        assertEquals("ORDER_COMPLETED", entity.getAction());
        assertEquals("supervisor@fleetops.com", entity.getActorEmail());

        AccessAuditLog mappedBack = auditMapper.toDomain(entity);
        assertNotNull(mappedBack);
        assertEquals(id, mappedBack.getId());
        assertEquals("ORDER_COMPLETED", mappedBack.getAction());
    }
}
