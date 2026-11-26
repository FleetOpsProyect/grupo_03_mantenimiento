package co.edu.univalle.fleetops.mantenimiento.domain.exception;

/**
 * Una dependencia externa (p. ej. MS Vehículos) no está disponible: timeout, error 5xx
 * o Circuit Breaker abierto. Se traduce a HTTP 503.
 */
public class ServiceUnavailableException extends DomainException {
    public ServiceUnavailableException(String errorCode, String message) {
        super(errorCode, message);
    }
}
