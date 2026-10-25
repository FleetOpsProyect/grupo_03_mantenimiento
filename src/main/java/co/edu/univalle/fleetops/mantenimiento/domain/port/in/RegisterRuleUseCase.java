package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;

public interface RegisterRuleUseCase {
    MaintenanceRule registerRule(MaintenanceRule rule);
}
