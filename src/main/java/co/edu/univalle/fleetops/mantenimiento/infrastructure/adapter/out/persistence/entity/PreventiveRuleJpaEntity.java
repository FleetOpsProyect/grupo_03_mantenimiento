package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "preventive_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreventiveRuleJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "scope_type", nullable = false)
    private String scopeType;

    @Column(name = "vehicle_category")
    private String vehicleCategory;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "mileage_interval_km", nullable = false)
    private Integer mileageIntervalKm;

    @Column(name = "time_interval_days", nullable = false)
    private Integer timeIntervalDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
