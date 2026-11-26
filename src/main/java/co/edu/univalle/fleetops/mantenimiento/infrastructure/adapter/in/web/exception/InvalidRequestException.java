package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.exception;

/** Un campo de la petición tiene un valor fuera del catálogo permitido. Se traduce a HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    private final String field;
    private final String code;

    public InvalidRequestException(String field, String code, String message) {
        super(message);
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }
}
