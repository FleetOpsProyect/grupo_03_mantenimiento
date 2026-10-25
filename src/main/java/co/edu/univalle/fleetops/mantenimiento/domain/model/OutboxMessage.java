package co.edu.univalle.fleetops.mantenimiento.domain.model;

import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
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
public class OutboxMessage {

    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String exchange;
    private String routingKey;
    private String payload;
    private OutboxStatus status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
