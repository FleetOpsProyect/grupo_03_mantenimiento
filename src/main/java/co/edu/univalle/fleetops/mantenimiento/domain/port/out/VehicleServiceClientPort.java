package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleServiceClientPort {

    record VehicleSummary(
            UUID id,
            Integer mileage,
            String status,
            VehicleCategory category
    ) {
        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status) || "ACTIVO".equalsIgnoreCase(status);
        }
    }

    List<VehicleSummary> getActiveVehicles();

    Optional<VehicleSummary> getVehicleById(UUID vehicleId);

    boolean updateVehicleStatus(UUID vehicleId, UUID maintenanceId, String newStatus);
}
