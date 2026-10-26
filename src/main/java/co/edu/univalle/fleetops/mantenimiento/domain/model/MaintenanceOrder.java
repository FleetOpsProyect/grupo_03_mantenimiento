package co.edu.univalle.fleetops.mantenimiento.domain.model;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.InvalidStateTransitionException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceOrder {

    private UUID id;
    private UUID vehicleId;
    private UUID technicianId;
    private UUID incidentId;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private MaintenanceOrigin origin;
    private Integer currentMileage;
    private String description;
    private String reason;
    private String assignedWorkshop;
    private BigDecimal totalCost;
    private String completedByType;
    private String completedById;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<MaintenanceLog> logs = new ArrayList<>();

    public void assignTechnician(UUID newTechnicianId) {
        if (this.status == MaintenanceStatus.COMPLETED || this.status == MaintenanceStatus.CANCELLED) {
            throw new InvalidStateTransitionException("No se puede asignar técnico a una orden finalizada o cancelada");
        }
        this.technicianId = newTechnicianId;
        this.updatedAt = LocalDateTime.now();
    }

    public void startProgress() {
        if (this.status != MaintenanceStatus.SCHEDULED_PENDING) {
            throw new InvalidStateTransitionException("Solo se puede iniciar una orden en estado SCHEDULED_PENDING");
        }
        this.status = MaintenanceStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String completedByType, String completedById, BigDecimal computedTotalCost) {
        if (this.status != MaintenanceStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Solo se puede completar una orden que esté EN_PROCESO (IN_PROGRESS)");
        }
        if (completedByType == null || completedByType.isBlank() || completedById == null || completedById.isBlank()) {
            throw new InvalidStateTransitionException("Se requiere el tipo y el identificador de quien completa la orden");
        }
        this.status = MaintenanceStatus.COMPLETED;
        this.completedByType = completedByType;
        this.completedById = completedById;
        this.completedAt = LocalDateTime.now();
        this.totalCost = (computedTotalCost != null) ? computedTotalCost : BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String cancelReason) {
        if (this.status == MaintenanceStatus.COMPLETED) {
            throw new InvalidStateTransitionException("No se puede cancelar una orden ya COMPLETADA");
        }
        this.status = MaintenanceStatus.CANCELLED;
        this.reason = cancelReason;
        this.updatedAt = LocalDateTime.now();
    }
}
