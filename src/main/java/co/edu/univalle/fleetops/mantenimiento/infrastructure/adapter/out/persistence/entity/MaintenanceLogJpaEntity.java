package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "maintenance_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_order_id", nullable = false)
    private MaintenanceOrderJpaEntity order;

    @Column(name = "author_type", nullable = false)
    private String authorType;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "log_type", nullable = false)
    private String logType;

    @Column(name = "notes", nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cost_delta", precision = 12, scale = 2)
    private BigDecimal costDelta;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Builder.Default
    @OneToMany(mappedBy = "maintenanceLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MaintenanceLogAttachmentJpaEntity> attachments = new ArrayList<>();

    public UUID getMaintenanceOrderId() {
        return order != null ? order.getId() : null;
    }
}
