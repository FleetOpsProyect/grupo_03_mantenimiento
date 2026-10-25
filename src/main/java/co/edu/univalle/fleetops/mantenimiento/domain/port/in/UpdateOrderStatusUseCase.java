package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;

import java.util.UUID;

public interface UpdateOrderStatusUseCase {
    MaintenanceOrder updateStatus(UUID orderId, MaintenanceStatus newStatus, String completedByType, String completedById);
    MaintenanceOrder assignTechnician(UUID orderId, UUID technicianId);
}
