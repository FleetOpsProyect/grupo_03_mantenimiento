package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;

import java.util.List;
import java.util.UUID;

public interface ProgressRepositoryPort {
    MaintenanceLog save(MaintenanceLog log);
    List<MaintenanceLog> findByOrderId(UUID orderId);
}
