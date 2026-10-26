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
                .aggregateType(entity.getAggregateType() != null ? entity.getAggregateType() : "MAINTENANCE_ORDER")
                .eventType(entity.getMessageType())
                .exchange(entity.getExchange())
                .routingKey(entity.getRoutingKey() != null ? entity.getRoutingKey() : entity.getDestination())
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
        String routingKey = domain.getRoutingKey() != null ? domain.getRoutingKey() : domain.getDestination();
        String destination = domain.getDestination() != null ? domain.getDestination() : routingKey;

        return OutboxMessageJpaEntity.builder()
                .id(domain.getId())
                .maintenanceOrderId(domain.getAggregateId())
                .aggregateType(domain.getAggregateType() != null ? domain.getAggregateType() : "MAINTENANCE_ORDER")
                .messageType(domain.getEventType() != null ? domain.getEventType() : "EVENT")
                .exchange(domain.getExchange())
                .routingKey(routingKey)
                .destination(destination != null ? destination : "unknown")
                .payload(domain.getPayload())
                .status(domain.getStatus() != null ? domain.getStatus().name() : OutboxStatus.PENDING.name())
                .attempts(domain.getRetryCount() != null ? domain.getRetryCount() : 0)
                .lastError(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getProcessedAt())
                .build();
    }
}
