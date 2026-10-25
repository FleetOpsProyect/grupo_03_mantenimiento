package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.OrderFilter;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.PagedOrdersResult;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    MaintenanceOrder save(MaintenanceOrder order);
    Optional<MaintenanceOrder> findById(UUID id);
    Optional<MaintenanceOrder> findActiveOrderByVehicleId(UUID vehicleId);
    Optional<MaintenanceOrder> findLatestCompletedOrderByVehicleId(UUID vehicleId);
    boolean existsByIncidentId(UUID incidentId);
    PagedOrdersResult queryOrders(OrderFilter filter);
}
