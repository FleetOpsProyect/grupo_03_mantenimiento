package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;

public interface RegisterProgressUseCase {
    MaintenanceLog registerProgress(MaintenanceLog log);
}
