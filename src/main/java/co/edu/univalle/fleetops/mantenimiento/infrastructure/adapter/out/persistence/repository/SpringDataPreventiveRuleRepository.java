package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.PreventiveRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPreventiveRuleRepository extends JpaRepository<PreventiveRuleJpaEntity, UUID> {

    @Query("SELECT r FROM PreventiveRuleJpaEntity r WHERE r.scopeType = :scope AND r.isActive = true")
    Optional<PreventiveRuleJpaEntity> findActiveGlobalRule(@Param("scope") String scope);

    @Query("SELECT r FROM PreventiveRuleJpaEntity r WHERE r.scopeType = 'CATEGORY' AND r.vehicleCategory = :category AND r.isActive = true")
    Optional<PreventiveRuleJpaEntity> findActiveCategoryRule(@Param("category") String category);

    @Query("SELECT r FROM PreventiveRuleJpaEntity r WHERE r.scopeType = 'VEHICLE' AND r.vehicleId = :vehicleId AND r.isActive = true")
    Optional<PreventiveRuleJpaEntity> findActiveVehicleRule(@Param("vehicleId") UUID vehicleId);
}
