package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato estándar de error de FleetOps:
 * {"codigo", "mensaje", "timestamp", "ruta", "errores": [{"campo", "codigo"}]}.
 * "errores" solo aparece en los 400 por validación.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String codigo,
        String mensaje,
        Instant timestamp,
        String ruta,
        List<FieldError> errores
) {

    public record FieldError(String campo, String codigo) {
    }

    public static ApiError of(String codigo, String mensaje, String ruta) {
        return new ApiError(codigo, mensaje, Instant.now(), ruta, null);
    }

    public static ApiError withErrors(String codigo, String mensaje, String ruta, List<FieldError> errores) {
        return new ApiError(codigo, mensaje, Instant.now(), ruta, errores);
    }
}
