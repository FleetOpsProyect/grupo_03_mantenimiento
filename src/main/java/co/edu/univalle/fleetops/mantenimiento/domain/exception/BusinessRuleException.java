package co.edu.univalle.fleetops.mantenimiento.domain.exception;

public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) {
        super("BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
