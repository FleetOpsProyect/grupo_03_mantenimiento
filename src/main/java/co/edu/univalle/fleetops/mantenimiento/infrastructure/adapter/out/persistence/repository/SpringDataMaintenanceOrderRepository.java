package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMaintenanceOrderRepository
        extends JpaRepository<MaintenanceOrderJpaEntity, UUID>, JpaSpecificationExecutor<MaintenanceOrderJpaEntity> {

    Optional<MaintenanceOrderJpaEntity> findFirstByVehicleIdAndStatusIn(
            UUID vehicleId,
            Collection<String> statuses
    );

    Optional<MaintenanceOrderJpaEntity> findFirstByVehicleIdAndStatusOrderByCompletedAtDesc(
            UUID vehicleId,
            String status
    );

    boolean existsByIncidentId(UUID incidentId);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at)) / 3600.0) " +
                   "FROM maintenance_order WHERE status = 'COMPLETED' AND completed_at IS NOT NULL",
           nativeQuery = true)
    Double calculateAverageRepairHours();
}
