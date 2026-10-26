package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

@Entity
@Table(name = "maintenance_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceOrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "technician_id")
    private UUID technicianId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "current_mileage")
    private Integer currentMileage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "assigned_workshop")
    private String assignedWorkshop;

    @Column(name = "completed_by_type")
    private String completedByType;

    @Column(name = "completed_by_id")
    private String completedById;

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MaintenanceLogJpaEntity> logs = new ArrayList<>();
}
