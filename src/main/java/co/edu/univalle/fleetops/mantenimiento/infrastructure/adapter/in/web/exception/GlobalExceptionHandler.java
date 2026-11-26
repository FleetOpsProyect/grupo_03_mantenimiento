package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.exception;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.BusinessRuleException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.DomainException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.InvalidStateTransitionException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ResourceConflictException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.ApiError;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduce las excepciones a respuestas HTTP con el formato estándar de error.
 * Spring elige el manejador del tipo más específico, por eso DataIntegrityViolationException (409)
 * gana sobre su superclase DataAccessException (503).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE_CASE =
            new PropertyNamingStrategies.SnakeCaseStrategy();
    private static final String INVALID_REQUEST_MESSAGE = "Estructura de solicitud inválida o campos requeridos ausentes";

    // ---------- 400: la petición está mal formada (no llega al dominio) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(SNAKE_CASE.translate(fe.getField()), fe.getCode()))
                .toList();
        return build(HttpStatus.BAD_REQUEST,
                ApiError.withErrors("VALIDATION_ERROR", INVALID_REQUEST_MESSAGE, req.getRequestURI(), errors));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = List.of(new ApiError.FieldError(ex.getField(), ex.getCode()));
        return build(HttpStatus.BAD_REQUEST,
                ApiError.withErrors("VALIDATION_ERROR", ex.getMessage(), req.getRequestURI(), errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        // JSON mal formado o tipos incorrectos (p. ej. un vehicle_id que no es UUID)
        return build(HttpStatus.BAD_REQUEST, ApiError.of("MALFORMED_REQUEST", INVALID_REQUEST_MESSAGE, req.getRequestURI()));
    }

    // ---------- Excepciones de dominio ----------

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        return domainError(HttpStatus.NOT_FOUND, ex, req);
    }

    @ExceptionHandler({ResourceConflictException.class, InvalidStateTransitionException.class})
    public ResponseEntity<ApiError> handleConflict(DomainException ex, HttpServletRequest req) {
        return domainError(HttpStatus.CONFLICT, ex, req);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        return domainError(HttpStatus.UNPROCESSABLE_ENTITY, ex, req);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleDependencyDown(ServiceUnavailableException ex, HttpServletRequest req) {
        LOG.error("Dependencia no disponible en {}: {}", req.getRequestURI(), ex.getMessage());
        return domainError(HttpStatus.SERVICE_UNAVAILABLE, ex, req);
    }

    // ---------- Base de datos ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        // P. ej. un índice único parcial rechazó una regla activa duplicada creada en paralelo
        LOG.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, ApiError.of("RESOURCE_CONFLICT",
                "El recurso entra en conflicto con uno existente", req.getRequestURI()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabaseDown(DataAccessException ex, HttpServletRequest req) {
        LOG.error("Fallo de base de datos en {}", req.getRequestURI(), ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ApiError.of("SERVICE_UNAVAILABLE",
                "Servicio no disponible temporalmente, intente más tarde", req.getRequestURI()));
    }

    // ---------- Cualquier otra cosa: 500 sin exponer trazas internas ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        // Las excepciones propias de Spring MVC (404 ruta inexistente, 405, 415...) traen su propio código
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status).body(ApiError.of("HTTP_" + status.value(),
                    errorResponse.getBody().getDetail(), req.getRequestURI()));
        }
        LOG.error("Error inesperado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiError.of("INTERNAL_ERROR",
                "Error interno del servidor", req.getRequestURI()));
    }

    private ResponseEntity<ApiError> domainError(HttpStatus status, DomainException ex, HttpServletRequest req) {
        return build(status, ApiError.of(ex.getErrorCode(), ex.getMessage(), req.getRequestURI()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ApiError body) {
        return ResponseEntity.status(status).body(body);
    }
}
