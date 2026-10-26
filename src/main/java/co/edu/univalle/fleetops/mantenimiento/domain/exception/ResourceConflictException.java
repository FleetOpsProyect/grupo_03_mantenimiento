package co.edu.univalle.fleetops.mantenimiento.domain.exception;

public class ResourceConflictException extends DomainException {
    public ResourceConflictException(String message) {
        super("RESOURCE_CONFLICT", message);
    }

    public ResourceConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
