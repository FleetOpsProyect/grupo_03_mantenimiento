package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.UUID;

/** Respuesta con la regla registrada, según la matriz de endpoints. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RuleResponse(
        UUID id,
        String scopeType,
        String vehicleCategory,
        UUID vehicleId,
        Integer mileageIntervalKm,
        Integer timeIntervalDays,
        String routineDescription,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
