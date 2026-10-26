package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

import co.edu.univalle.fleetops.mantenimiento.domain.model.OutboxMessage;

import java.util.List;
import java.util.UUID;

public interface OutboxRepositoryPort {

    OutboxMessage save(OutboxMessage message);

    List<OutboxMessage> findPendingMessages(int limit);

    void markAsSent(UUID messageId);

    void markAsFailed(UUID messageId, String errorMessage);
}
