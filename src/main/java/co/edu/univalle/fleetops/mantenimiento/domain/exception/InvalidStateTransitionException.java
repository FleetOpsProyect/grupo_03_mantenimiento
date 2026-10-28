package co.edu.univalle.fleetops.mantenimiento.domain.exception;

public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(String message) {
        super("INVALID_TRANSITION", message);
    }
}
