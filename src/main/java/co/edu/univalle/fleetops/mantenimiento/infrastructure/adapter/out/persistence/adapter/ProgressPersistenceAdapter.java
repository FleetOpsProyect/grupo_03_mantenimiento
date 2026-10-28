package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLogAttachment;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.ProgressRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.MaintenanceLogPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProgressPersistenceAdapter implements ProgressRepositoryPort {

    private final SpringDataMaintenanceLogRepository repository;
    private final MaintenanceLogPersistenceMapper mapper;

    public ProgressPersistenceAdapter(SpringDataMaintenanceLogRepository repository, MaintenanceLogPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MaintenanceLog save(MaintenanceLog log) {
        if (log.getId() == null) {
            log.setId(UUID.randomUUID());
        }
        if (log.getRegisteredAt() == null) {
            log.setRegisteredAt(LocalDateTime.now());
        }

        if (log.getAttachments() != null) {
            for (MaintenanceLogAttachment att : log.getAttachments()) {
                if (att.getId() == null) {
                    att.setId(UUID.randomUUID());
                }
                if (att.getMaintenanceLogId() == null) {
                    att.setMaintenanceLogId(log.getId());
                }
                if (att.getCreatedAt() == null) {
                    att.setCreatedAt(LocalDateTime.now());
                }
            }
        }

        MaintenanceLogJpaEntity entity = mapper.toEntity(log);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceLog> findByOrderId(UUID orderId) {
        return repository.findByOrderIdWithAttachments(orderId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
