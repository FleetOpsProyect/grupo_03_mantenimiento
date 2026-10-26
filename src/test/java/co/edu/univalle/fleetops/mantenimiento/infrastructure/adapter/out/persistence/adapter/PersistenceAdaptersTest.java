package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.OutboxMessage;
import co.edu.univalle.fleetops.mantenimiento.domain.model.Technician;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.OrderFilter;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.PagedOrdersResult;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.OutboxMessageJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.PreventiveRuleJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.MaintenanceLogPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.MaintenanceOrderPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.OutboxMessagePersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.PreventiveRulePersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.TechnicianPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceLogRepository;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceOrderRepository;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataOutboxMessageRepository;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataPreventiveRuleRepository;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataTechnicianRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Persistence Adapters Unit Tests")
class PersistenceAdaptersTest {

    // --- Technician Adapter ---
    @Mock
    private SpringDataTechnicianRepository technicianRepository;
    @Spy
    private TechnicianPersistenceMapper technicianMapper = new TechnicianPersistenceMapper();

    // --- Rule Adapter ---
    @Mock
    private SpringDataPreventiveRuleRepository ruleRepository;
    @Spy
    private PreventiveRulePersistenceMapper ruleMapper = new PreventiveRulePersistenceMapper();

    // --- Order Adapter ---
    @Mock
    private SpringDataMaintenanceOrderRepository orderRepository;
    @Spy
    private MaintenanceLogPersistenceMapper logMapper = new MaintenanceLogPersistenceMapper();
    @Spy
    private MaintenanceOrderPersistenceMapper orderMapper = new MaintenanceOrderPersistenceMapper(new MaintenanceLogPersistenceMapper());

    // --- Progress Adapter ---
    @Mock
    private SpringDataMaintenanceLogRepository progressRepository;

    // --- Outbox Adapter ---
    @Mock
    private SpringDataOutboxMessageRepository outboxRepository;
    @Spy
    private OutboxMessagePersistenceMapper outboxMapper = new OutboxMessagePersistenceMapper();

    @Test
    @DisplayName("TechnicianPersistenceAdapter operations")
    void testTechnicianAdapter() {
        TechnicianPersistenceAdapter adapter = new TechnicianPersistenceAdapter(technicianRepository, technicianMapper);

        UUID techId = UUID.randomUUID();
        Technician domain = Technician.builder()
                .name("Pedro")
                .email("pedro@fleetops.com")
                .phoneNumber("+573001234567")
                .isActive(true)
                .build();

        TechnicianJpaEntity savedEntity = TechnicianJpaEntity.builder()
                .id(techId)
                .name("Pedro")
                .email("pedro@fleetops.com")
                .phoneNumber("+573001234567")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(technicianRepository.save(any(TechnicianJpaEntity.class))).thenReturn(savedEntity);
        when(technicianRepository.findById(techId)).thenReturn(Optional.of(savedEntity));
        when(technicianRepository.findByEmail("pedro@fleetops.com")).thenReturn(Optional.of(savedEntity));
        when(technicianRepository.existsByEmail("pedro@fleetops.com")).thenReturn(true);

        Technician saved = adapter.save(domain);
        assertNotNull(saved);
        assertEquals(techId, saved.getId());

        Optional<Technician> foundById = adapter.findById(techId);
        assertTrue(foundById.isPresent());
        assertEquals("pedro@fleetops.com", foundById.get().getEmail());

        Optional<Technician> foundByEmail = adapter.findByEmail("pedro@fleetops.com");
        assertTrue(foundByEmail.isPresent());

        assertTrue(adapter.existsByEmail("pedro@fleetops.com"));

        // Test findAll with isActive filter
        Page<TechnicianJpaEntity> page = new PageImpl<>(List.of(savedEntity));
        when(technicianRepository.findByIsActive(eq(true), any(PageRequest.class))).thenReturn(page);
        when(technicianRepository.findAll(any(PageRequest.class))).thenReturn(page);

        List<Technician> activeList = adapter.findAll(true, 0, 10);
        assertEquals(1, activeList.size());

        List<Technician> allList = adapter.findAll(null, -1, 200);
        assertEquals(1, allList.size());
    }

    @Test
    @DisplayName("RulePersistenceAdapter precedence and scopes")
    void testRuleAdapter() {
        RulePersistenceAdapter adapter = new RulePersistenceAdapter(ruleRepository, ruleMapper);

        UUID ruleId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        PreventiveRuleJpaEntity globalEntity = PreventiveRuleJpaEntity.builder()
                .id(UUID.randomUUID())
                .scopeType("GLOBAL")
                .mileageIntervalKm(10000)
                .timeIntervalDays(180)
                .isActive(true)
                .build();

        PreventiveRuleJpaEntity catEntity = PreventiveRuleJpaEntity.builder()
                .id(UUID.randomUUID())
                .scopeType("CATEGORY")
                .vehicleCategory("CARGA_PESADA")
                .mileageIntervalKm(8000)
                .timeIntervalDays(90)
                .isActive(true)
                .build();

        PreventiveRuleJpaEntity vehEntity = PreventiveRuleJpaEntity.builder()
                .id(ruleId)
                .scopeType("VEHICLE")
                .vehicleId(vehicleId)
                .mileageIntervalKm(5000)
                .timeIntervalDays(60)
                .isActive(true)
                .build();

        when(ruleRepository.save(any(PreventiveRuleJpaEntity.class))).thenReturn(vehEntity);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(vehEntity));
        when(ruleRepository.findAll()).thenReturn(List.of(globalEntity, catEntity, vehEntity));

        // Save
        MaintenanceRule saved = adapter.save(MaintenanceRule.builder().scope(RuleScope.VEHICLE).vehicleId(vehicleId).build());
        assertNotNull(saved);
        assertEquals(ruleId, saved.getId());

        // Find by id
        assertTrue(adapter.findById(ruleId).isPresent());
        assertEquals(3, adapter.findAll().size());

        // Find active rule by scope
        when(ruleRepository.findActiveGlobalRule("GLOBAL")).thenReturn(Optional.of(globalEntity));
        when(ruleRepository.findActiveCategoryRule("CARGA_PESADA")).thenReturn(Optional.of(catEntity));
        when(ruleRepository.findActiveVehicleRule(vehicleId)).thenReturn(Optional.of(vehEntity));

        assertTrue(adapter.findActiveRule(RuleScope.GLOBAL, null, null).isPresent());
        assertTrue(adapter.findActiveRule(RuleScope.CATEGORY, VehicleCategory.CARGA_PESADA, null).isPresent());
        assertTrue(adapter.findActiveRule(RuleScope.VEHICLE, null, vehicleId).isPresent());
        assertFalse(adapter.findActiveRule(RuleScope.CATEGORY, null, null).isPresent());

        // Precedence: 1. vehicle
        Optional<MaintenanceRule> applicableVeh = adapter.findApplicableRule(vehicleId, VehicleCategory.CARGA_PESADA);
        assertTrue(applicableVeh.isPresent());
        assertEquals(RuleScope.VEHICLE, applicableVeh.get().getScope());

        // Precedence: 2. category (when vehicle rule not found)
        when(ruleRepository.findActiveVehicleRule(vehicleId)).thenReturn(Optional.empty());
        Optional<MaintenanceRule> applicableCat = adapter.findApplicableRule(vehicleId, VehicleCategory.CARGA_PESADA);
        assertTrue(applicableCat.isPresent());
        assertEquals(RuleScope.CATEGORY, applicableCat.get().getScope());

        // Precedence: 3. global (when neither vehicle nor category rule found)
        when(ruleRepository.findActiveCategoryRule("CARGA_PESADA")).thenReturn(Optional.empty());
        Optional<MaintenanceRule> applicableGlobal = adapter.findApplicableRule(vehicleId, VehicleCategory.CARGA_PESADA);
        assertTrue(applicableGlobal.isPresent());
        assertEquals(RuleScope.GLOBAL, applicableGlobal.get().getScope());
    }

    @Test
    @DisplayName("OrderPersistenceAdapter queries, stats and status")
    void testOrderAdapter() {
        OrderPersistenceAdapter adapter = new OrderPersistenceAdapter(orderRepository, orderMapper);

        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        MaintenanceOrderJpaEntity entity = MaintenanceOrderJpaEntity.builder()
                .id(orderId)
                .vehicleId(vehicleId)
                .technicianId(technicianId)
                .incidentId(incidentId)
                .maintenanceType("PREVENTIVE")
                .status("IN_PROGRESS")
                .origin("SCHEDULER")
                .currentMileage(20000)
                .totalCost(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .logs(new ArrayList<>())
                .build();

        when(orderRepository.save(any(MaintenanceOrderJpaEntity.class))).thenReturn(entity);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(entity));
        when(orderRepository.findFirstByVehicleIdAndStatusIn(eq(vehicleId), any())).thenReturn(Optional.of(entity));
        when(orderRepository.findFirstByVehicleIdAndStatusOrderByCompletedAtDesc(eq(vehicleId), eq("COMPLETED"))).thenReturn(Optional.of(entity));
        when(orderRepository.existsByIncidentId(incidentId)).thenReturn(true);

        MaintenanceOrder saved = adapter.save(MaintenanceOrder.builder().vehicleId(vehicleId).build());
        assertNotNull(saved);
        assertEquals(orderId, saved.getId());

        assertTrue(adapter.findById(orderId).isPresent());
        assertTrue(adapter.findActiveOrderByVehicleId(vehicleId).isPresent());
        assertTrue(adapter.findLatestCompletedOrderByVehicleId(vehicleId).isPresent());
        assertTrue(adapter.existsByIncidentId(incidentId));

        // Test queryOrders with filter
        Page<MaintenanceOrderJpaEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(orderRepository.calculateAverageRepairHours()).thenReturn(4.55);

        OrderFilter filter = new OrderFilter(
                MaintenanceStatus.IN_PROGRESS,
                MaintenanceType.PREVENTIVE,
                vehicleId,
                technicianId,
                1,
                10
        );

        PagedOrdersResult result = adapter.queryOrders(filter);
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(1, result.page());
        assertEquals(1, result.totalElements());
        assertEquals(4.55, result.summary().get("avg_repair_hours"));

        // Query with null filter values
        OrderFilter emptyFilter = new OrderFilter(null, null, null, null, 0, 0);
        when(orderRepository.calculateAverageRepairHours()).thenReturn(null);
        PagedOrdersResult emptyResult = adapter.queryOrders(emptyFilter);
        assertNotNull(emptyResult);
    }

    @Test
    @DisplayName("ProgressPersistenceAdapter saves and retrieves logs")
    void testProgressAdapter() {
        ProgressPersistenceAdapter adapter = new ProgressPersistenceAdapter(progressRepository, logMapper);

        UUID logId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        MaintenanceLogJpaEntity entity = MaintenanceLogJpaEntity.builder()
                .id(logId)
                .maintenanceOrderId(orderId)
                .logType("REVISION")
                .notes("Revisión visual OK")
                .registeredAt(LocalDateTime.now())
                .attachments(new ArrayList<>())
                .build();

        when(progressRepository.save(any(MaintenanceLogJpaEntity.class))).thenReturn(entity);
        when(progressRepository.findByMaintenanceOrderIdOrderByRegisteredAtAsc(orderId)).thenReturn(List.of(entity));

        MaintenanceLog saved = adapter.save(MaintenanceLog.builder().orderId(orderId).notes("Revisión visual OK").build());
        assertNotNull(saved);
        assertEquals(logId, saved.getId());

        List<MaintenanceLog> logs = adapter.findByOrderId(orderId);
        assertEquals(1, logs.size());
        assertEquals("Revisión visual OK", logs.get(0).getNotes());
    }

    @Test
    @DisplayName("OutboxPersistenceAdapter saves, marks sent, retries and marks failed")
    void testOutboxAdapter() {
        OutboxPersistenceAdapter adapter = new OutboxPersistenceAdapter(outboxRepository, outboxMapper);

        UUID messageId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OutboxMessageJpaEntity entity = OutboxMessageJpaEntity.builder()
                .id(messageId)
                .maintenanceOrderId(orderId)
                .messageType("maintenance.order.created")
                .destination("maintenance-exchange")
                .payload("{}")
                .status("PENDING")
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxRepository.save(any(OutboxMessageJpaEntity.class))).thenReturn(entity);
        when(outboxRepository.findPendingToProcess(any(), any())).thenReturn(List.of(entity));

        OutboxMessage saved = adapter.save(OutboxMessage.builder().aggregateId(orderId).build());
        assertNotNull(saved);
        assertEquals(messageId, saved.getId());

        List<OutboxMessage> pending = adapter.findPendingMessages(5);
        assertEquals(1, pending.size());

        // Mark as sent
        when(outboxRepository.findById(messageId)).thenReturn(Optional.of(entity));
        adapter.markAsSent(messageId);
        verify(outboxRepository).save(entity);
        assertEquals(OutboxStatus.SENT.name(), entity.getStatus());

        // Mark as failed - attempt 1 (retry exponential backoff)
        entity.setAttempts(0);
        adapter.markAsFailed(messageId, "Broker connection timeout");
        assertEquals(1, entity.getAttempts());
        assertEquals("Broker connection timeout", entity.getLastError());
        assertNotNull(entity.getNextAttemptAt());

        // Mark as failed - attempt 3 (marks status FAILED)
        entity.setAttempts(2);
        adapter.markAsFailed(messageId, "Fatal message format");
        assertEquals(3, entity.getAttempts());
        assertEquals(OutboxStatus.FAILED.name(), entity.getStatus());
    }
}
