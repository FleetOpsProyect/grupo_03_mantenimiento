package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.RegisterRuleRequest;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.RuleResponse;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Traduce entre el contrato HTTP (DTO) y el dominio.
 * La API usa los valores del contrato en español (GLOBAL | CATEGORIA | VEHICULO); el dominio usa
 * el enum en inglés. Se aceptan ambas formas en la entrada y se responde con la del contrato.
 */
@Component
public class RuleWebMapper {

    public MaintenanceRule toDomain(RegisterRuleRequest request) {
        return MaintenanceRule.builder()
                .scope(parseScope(request.scopeType()))
                .category(parseCategory(request.vehicleCategory()))
                .vehicleId(request.vehicleId())
                .mileageIntervalKm(request.mileageIntervalKm())
                .timeIntervalDays(request.timeIntervalDays())
                .routineDescription(request.routineDescription())
                .isActive(request.isActive())
                .build();
    }

    public RuleResponse toResponse(MaintenanceRule rule) {
        return new RuleResponse(
                rule.getId(),
                toApiScope(rule.getScope()),
                rule.getCategory() != null ? rule.getCategory().name() : null,
                rule.getVehicleId(),
                rule.getMileageIntervalKm(),
                rule.getTimeIntervalDays(),
                rule.getRoutineDescription(),
                rule.getIsActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private RuleScope parseScope(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GLOBAL" -> RuleScope.GLOBAL;
            case "CATEGORIA", "CATEGORY" -> RuleScope.CATEGORY;
            case "VEHICULO", "VEHICLE" -> RuleScope.VEHICLE;
            default -> throw new InvalidRequestException("scope_type", "INVALID_VALUE",
                    "scope_type debe ser GLOBAL, CATEGORIA o VEHICULO");
        };
    }

    private VehicleCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return VehicleCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("vehicle_category", "INVALID_VALUE",
                    "vehicle_category debe ser CAMIONETA, AUTOMOVIL o CARGA_PESADA");
        }
    }

    private String toApiScope(RuleScope scope) {
        if (scope == null) {
            return null;
        }
        return switch (scope) {
            case GLOBAL -> "GLOBAL";
            case CATEGORY -> "CATEGORIA";
            case VEHICLE -> "VEHICULO";
        };
    }
}
