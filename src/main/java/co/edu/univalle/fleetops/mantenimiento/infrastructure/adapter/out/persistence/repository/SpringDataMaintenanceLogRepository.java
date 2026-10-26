package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataMaintenanceLogRepository extends JpaRepository<MaintenanceLogJpaEntity, UUID> {
    List<MaintenanceLogJpaEntity> findByMaintenanceOrderIdOrderByRegisteredAtAsc(UUID maintenanceOrderId);
}
