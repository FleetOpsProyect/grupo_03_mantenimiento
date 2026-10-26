package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogAttachmentJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceLogRepository;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JpaPersistenceContextIntegrationTest {

    @Autowired
    private SpringDataMaintenanceOrderRepository orderRepository;

    @Autowired
    private SpringDataMaintenanceLogRepository logRepository;

    @Test
    @Transactional
    @DisplayName("Debe persistir y recuperar grafos de entidades con OneToMany/ManyToOne bidireccionales")
    void shouldPersistAndLoadEntityGraph() {
        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        MaintenanceOrderJpaEntity order = MaintenanceOrderJpaEntity.builder()
                .id(orderId)
                .vehicleId(vehicleId)
                .maintenanceType("PREVENTIVE")
                .status("IN_PROGRESS")
                .origin("SCHEDULER")
                .currentMileage(30000)
                .description("Revisión de 30,000 km")
                .assignedWorkshop("Taller Central")
                .totalCost(BigDecimal.valueOf(180000))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UUID logId = UUID.randomUUID();
        MaintenanceLogJpaEntity log = MaintenanceLogJpaEntity.builder()
                .id(logId)
                .order(order)
                .logType("PROGRESS")
                .notes("Revisión de frenos y cambio de líquido")
                .authorType("TECHNICIAN")
                .authorId(UUID.randomUUID().toString())
                .authorName("Carlos Ruiz")
                .registeredAt(LocalDateTime.now())
                .build();

        UUID attachId = UUID.randomUUID();
        MaintenanceLogAttachmentJpaEntity attachment = MaintenanceLogAttachmentJpaEntity.builder()
                .id(attachId)
                .maintenanceLog(log)
                .fileName("frenos_delanteros.png")
                .fileUrl("https://storage.fleetops.com/attachments/frenos_delanteros.png")
                .fileSizeBytes(204800L)
                .createdAt(LocalDateTime.now())
                .build();

        log.setAttachments(List.of(attachment));
        order.setLogs(List.of(log));

        MaintenanceOrderJpaEntity savedOrder = orderRepository.save(order);
        assertNotNull(savedOrder);
        assertEquals(orderId, savedOrder.getId());

        Optional<MaintenanceOrderJpaEntity> reloaded = orderRepository.findById(orderId);
        assertTrue(reloaded.isPresent());
        assertEquals("Taller Central", reloaded.get().getAssignedWorkshop());
        assertEquals(1, reloaded.get().getLogs().size());

        List<MaintenanceLogJpaEntity> logs = logRepository.findByOrderIdWithAttachments(orderId);
        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals(1, logs.get(0).getAttachments().size());
        assertEquals("frenos_delanteros.png", logs.get(0).getAttachments().get(0).getFileName());
    }
}
