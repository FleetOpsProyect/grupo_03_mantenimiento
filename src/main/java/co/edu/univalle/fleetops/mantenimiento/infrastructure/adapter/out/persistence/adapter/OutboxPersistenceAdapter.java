package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.OutboxMessage;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.OutboxStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OutboxRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.OutboxMessageJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.OutboxMessagePersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataOutboxMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OutboxPersistenceAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxMessageRepository repository;
    private final OutboxMessagePersistenceMapper mapper;

    public OutboxPersistenceAdapter(SpringDataOutboxMessageRepository repository, OutboxMessagePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OutboxMessage save(OutboxMessage message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID());
        }
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        if (message.getStatus() == null) {
            message.setStatus(OutboxStatus.PENDING);
        }
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }

        OutboxMessageJpaEntity entity = mapper.toEntity(message);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<OutboxMessage> findPendingMessages(int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.min(100, Math.max(1, limit)));
        return repository.findPendingToProcess(LocalDateTime.now(), pageRequest)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsSent(UUID messageId) {
        repository.findById(messageId).ifPresent(entity -> {
            entity.setStatus(OutboxStatus.SENT.name());
            entity.setSentAt(LocalDateTime.now());
            repository.save(entity);
        });
    }

    @Override
    public void markAsFailed(UUID messageId, String errorMessage) {
        repository.findById(messageId).ifPresent(entity -> {
            entity.setAttempts(entity.getAttempts() + 1);
            entity.setLastError(errorMessage);
            if (entity.getAttempts() >= 3) {
                entity.setStatus(OutboxStatus.FAILED.name());
            } else {
                // Backoff exponencial para el siguiente intento
                entity.setNextAttemptAt(LocalDateTime.now().plusSeconds(10L * entity.getAttempts()));
            }
            repository.save(entity);
        });
    }
}
