package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.RuleRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.PreventiveRuleJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.PreventiveRulePersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataPreventiveRuleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RulePersistenceAdapter implements RuleRepositoryPort {

    private final SpringDataPreventiveRuleRepository repository;
    private final PreventiveRulePersistenceMapper mapper;

    public RulePersistenceAdapter(SpringDataPreventiveRuleRepository repository, PreventiveRulePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public MaintenanceRule save(MaintenanceRule rule) {
        rule.validate();

        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID());
        }
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(LocalDateTime.now());
        }
        rule.setUpdatedAt(LocalDateTime.now());

        PreventiveRuleJpaEntity entity = mapper.toEntity(rule);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<MaintenanceRule> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MaintenanceRule> findActiveRule(RuleScope scope, VehicleCategory category, UUID vehicleId) {
        if (scope == RuleScope.GLOBAL) {
            return repository.findActiveGlobalRule("GLOBAL").map(mapper::toDomain);
        }
        if (scope == RuleScope.CATEGORY && category != null) {
            return repository.findActiveCategoryRule(category.name()).map(mapper::toDomain);
        }
        if (scope == RuleScope.VEHICLE && vehicleId != null) {
            return repository.findActiveVehicleRule(vehicleId).map(mapper::toDomain);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MaintenanceRule> findApplicableRule(UUID vehicleId, VehicleCategory category) {
        // 1. Precedencia: Regla específica por vehículo
        if (vehicleId != null) {
            Optional<PreventiveRuleJpaEntity> vehicleRule = repository.findActiveVehicleRule(vehicleId);
            if (vehicleRule.isPresent()) {
                return vehicleRule.map(mapper::toDomain);
            }
        }

        // 2. Precedencia: Regla por categoría
        if (category != null) {
            Optional<PreventiveRuleJpaEntity> categoryRule = repository.findActiveCategoryRule(category.name());
            if (categoryRule.isPresent()) {
                return categoryRule.map(mapper::toDomain);
            }
        }

        // 3. Precedencia: Regla global por defecto
        return repository.findActiveGlobalRule("GLOBAL").map(mapper::toDomain);
    }

    @Override
    public List<MaintenanceRule> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
