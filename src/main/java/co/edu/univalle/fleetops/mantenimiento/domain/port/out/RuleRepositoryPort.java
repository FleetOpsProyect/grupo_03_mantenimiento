package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleRepositoryPort {
    MaintenanceRule save(MaintenanceRule rule);
    Optional<MaintenanceRule> findById(UUID id);
    Optional<MaintenanceRule> findActiveRule(RuleScope scope, VehicleCategory category, UUID vehicleId);
    Optional<MaintenanceRule> findApplicableRule(UUID vehicleId, VehicleCategory category);
    List<MaintenanceRule> findAll();
}
