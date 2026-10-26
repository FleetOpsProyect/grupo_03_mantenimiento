package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"logs"})
    @Override
    Optional<MaintenanceOrderJpaEntity> findById(UUID id);

    Optional<MaintenanceOrderJpaEntity> findFirstByVehicleIdAndStatusIn(
            UUID vehicleId,
            Collection<String> statuses
    );

    Optional<MaintenanceOrderJpaEntity> findFirstByVehicleIdAndMaintenanceTypeAndStatusInOrderByCreatedAtDesc(
            UUID vehicleId,
            String maintenanceType,
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

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at)) / 3600.0) " +
                   "FROM maintenance_order WHERE status = 'COMPLETED' AND completed_at IS NOT NULL " +
                   "AND (CAST(:vehicleId AS UUID) IS NULL OR vehicle_id = :vehicleId) " +
                   "AND (CAST(:technicianId AS UUID) IS NULL OR technician_id = :technicianId) " +
                   "AND (:maintenanceType IS NULL OR maintenance_type = :maintenanceType)",
           nativeQuery = true)
    Double calculateAverageRepairHoursFiltered(
            @Param("vehicleId") UUID vehicleId,
            @Param("technicianId") UUID technicianId,
            @Param("maintenanceType") String maintenanceType
    );
}
