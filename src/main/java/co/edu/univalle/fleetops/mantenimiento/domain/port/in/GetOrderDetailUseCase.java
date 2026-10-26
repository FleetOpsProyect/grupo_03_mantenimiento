package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;

import java.util.UUID;

public interface GetOrderDetailUseCase {
    MaintenanceOrder getOrderDetail(UUID orderId);
}
