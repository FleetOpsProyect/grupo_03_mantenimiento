package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.PreventiveRuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PreventiveRulePersistenceMapper {

    public MaintenanceRule toDomain(PreventiveRuleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return MaintenanceRule.builder()
                .id(entity.getId())
                .scope(entity.getScopeType() != null ? RuleScope.valueOf(entity.getScopeType()) : null)
                .category(entity.getVehicleCategory() != null ? VehicleCategory.valueOf(entity.getVehicleCategory()) : null)
                .vehicleId(entity.getVehicleId())
                .mileageIntervalKm(entity.getMileageIntervalKm())
                .timeIntervalDays(entity.getTimeIntervalDays())
                .routineDescription(entity.getRoutineDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PreventiveRuleJpaEntity toEntity(MaintenanceRule domain) {
        if (domain == null) {
            return null;
        }
        return PreventiveRuleJpaEntity.builder()
                .id(domain.getId())
                .scopeType(domain.getScope() != null ? domain.getScope().name() : null)
                .vehicleCategory(domain.getCategory() != null ? domain.getCategory().name() : null)
                .vehicleId(domain.getVehicleId())
                .mileageIntervalKm(domain.getMileageIntervalKm())
                .timeIntervalDays(domain.getTimeIntervalDays())
                .routineDescription(domain.getRoutineDescription())
                .isActive(domain.getIsActive() != null ? domain.getIsActive() : true)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
