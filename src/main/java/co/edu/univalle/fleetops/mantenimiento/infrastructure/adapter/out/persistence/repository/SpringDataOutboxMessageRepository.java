package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.OutboxMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataOutboxMessageRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_message " +
                   "WHERE status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= :now) " +
                   "ORDER BY created_at ASC " +
                   "LIMIT :limit FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    List<OutboxMessageJpaEntity> findPendingToProcessForUpdate(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Query("SELECT m FROM OutboxMessageJpaEntity m WHERE m.status = 'PENDING' AND (m.nextAttemptAt IS NULL OR m.nextAttemptAt <= :now) ORDER BY m.createdAt ASC")
    List<OutboxMessageJpaEntity> findPendingToProcess(@Param("now") LocalDateTime now, Pageable pageable);
}
