package co.edu.univalle.fleetops.mantenimiento.domain.model;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.BusinessRuleException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRule {

    private UUID id;
    private RuleScope scope;
    private VehicleCategory category;
    private UUID vehicleId;
    private Integer thresholdKm;
    private Integer thresholdDays;
    private String routineDescription;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (scope == null) {
            throw new BusinessRuleException("INVALID_SCOPE", "El alcance de la regla es obligatorio");
        }
        if (scope == RuleScope.CATEGORY && category == null) {
            throw new BusinessRuleException("CATEGORY_REQUIRED", "La categoría es obligatoria para reglas de alcance CATEGORIA");
        }
        if (scope == RuleScope.VEHICLE && vehicleId == null) {
            throw new BusinessRuleException("VEHICLE_REQUIRED", "El ID del vehículo es obligatorio para reglas de alcance VEHICULO");
        }
        if (thresholdKm == null || thresholdKm < 1000 || thresholdKm > 50000) {
            throw new BusinessRuleException("INVALID_KM_THRESHOLD", "El umbral de kilometraje debe estar entre 1,000 y 50,000 km");
        }
        if (thresholdDays == null || thresholdDays < 30 || thresholdDays > 365) {
            throw new BusinessRuleException("INVALID_DAYS_THRESHOLD", "El umbral de días debe estar entre 30 y 365 días");
        }
    }
}
