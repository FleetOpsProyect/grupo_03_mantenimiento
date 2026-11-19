package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Cuerpo de POST /maintenance/rules. Llega en snake_case (scope_type, vehicle_id, ...).
 * Aquí solo se valida la forma (400); los rangos de umbrales los valida el dominio (422).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterRuleRequest(
        @NotBlank String scopeType,
        String vehicleCategory,
        UUID vehicleId,
        @NotNull Integer mileageIntervalKm,
        @NotNull Integer timeIntervalDays,
        @Size(max = 500) String routineDescription,
        Boolean isActive
) {
}
