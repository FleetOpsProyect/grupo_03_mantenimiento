package co.edu.univalle.fleetops.mantenimiento.domain.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String message) {
        super("ENTITY_NOT_FOUND", message);
    }

    public EntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
