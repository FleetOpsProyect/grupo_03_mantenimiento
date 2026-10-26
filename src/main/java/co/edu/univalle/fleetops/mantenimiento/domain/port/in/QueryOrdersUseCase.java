package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface QueryOrdersUseCase {

    record OrderFilter(
            MaintenanceStatus status,
            MaintenanceType maintenanceType,
            UUID vehicleId,
            UUID technicianId,
            int page,
            int size
    ) {}

    record PagedOrdersResult(
            List<MaintenanceOrder> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            Map<String, Object> summary
    ) {}

    PagedOrdersResult queryOrders(OrderFilter filter);
}
