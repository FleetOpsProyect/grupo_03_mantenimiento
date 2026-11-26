package co.edu.univalle.fleetops.mantenimiento.domain.exception;

public class VehicleSyncException extends DomainException {

    public VehicleSyncException(String message) {
        super("VEHICLE_SYNC_ERROR", message);
    }

    public VehicleSyncException(String message, Throwable cause) {
        super("VEHICLE_SYNC_ERROR", message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
