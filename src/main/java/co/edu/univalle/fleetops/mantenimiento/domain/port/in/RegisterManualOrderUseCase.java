package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;

public interface RegisterManualOrderUseCase {
    MaintenanceOrder registerManualOrder(MaintenanceOrder order);
}
