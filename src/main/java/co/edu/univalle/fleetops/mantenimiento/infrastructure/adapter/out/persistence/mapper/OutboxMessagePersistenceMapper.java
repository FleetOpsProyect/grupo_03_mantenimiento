package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.OutboxMessage;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.OutboxMessageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxMessagePersistenceMapper {

    public OutboxMessage toDomain(OutboxMessageJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return OutboxMessage.builder()
                .id(entity.getId())
                .aggregateId(entity.getMaintenanceOrderId())
                .eventType(entity.getMessageType())
                .destination(entity.getDestination())
                .payload(entity.getPayload())
                .status(entity.getStatus() != null ? OutboxStatus.valueOf(entity.getStatus()) : null)
                .retryCount(entity.getAttempts())
                .errorMessage(entity.getLastError())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getSentAt())
                .build();
    }

    public OutboxMessageJpaEntity toEntity(OutboxMessage domain) {
        if (domain == null) {
            return null;
        }
        return OutboxMessageJpaEntity.builder()
                .id(domain.getId())
                .maintenanceOrderId(domain.getAggregateId())
                .messageType(domain.getEventType() != null ? domain.getEventType() : "EVENT")
                .destination(domain.getDestination() != null ? domain.getDestination() : domain.getRoutingKey())
                .payload(domain.getPayload())
                .status(domain.getStatus() != null ? domain.getStatus().name() : OutboxStatus.PENDING.name())
                .attempts(domain.getRetryCount() != null ? domain.getRetryCount() : 0)
                .lastError(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getProcessedAt())
                .build();
    }
}
