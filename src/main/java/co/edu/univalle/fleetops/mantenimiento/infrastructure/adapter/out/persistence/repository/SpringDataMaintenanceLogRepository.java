package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataMaintenanceLogRepository extends JpaRepository<MaintenanceLogJpaEntity, UUID> {

    @Query("SELECT DISTINCT l FROM MaintenanceLogJpaEntity l LEFT JOIN FETCH l.attachments WHERE l.order.id = :orderId ORDER BY l.registeredAt ASC")
    List<MaintenanceLogJpaEntity> findByOrderIdWithAttachments(@Param("orderId") UUID orderId);
}
