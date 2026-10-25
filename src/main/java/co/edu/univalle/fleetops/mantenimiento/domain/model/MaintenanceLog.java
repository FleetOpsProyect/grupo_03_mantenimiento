package co.edu.univalle.fleetops.mantenimiento.domain.model;

import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.LogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceLog {

    private UUID id;
    private UUID orderId;
    private LogType logType;
    private String notes;
    private BigDecimal costDelta;
    private String authorType;
    private String authorId;
    private String authorName;
    private LocalDateTime registeredAt;
}
