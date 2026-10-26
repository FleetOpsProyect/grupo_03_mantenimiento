package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.ProgressRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.MaintenanceLogPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceLogRepository;
import org.springframework.stereotype.Component;

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
    public MaintenanceLog save(MaintenanceLog log) {
        if (log.getId() == null) {
            log.setId(UUID.randomUUID());
        }
        if (log.getRegisteredAt() == null) {
            log.setRegisteredAt(LocalDateTime.now());
        }

        MaintenanceLogJpaEntity entity = mapper.toEntity(log);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<MaintenanceLog> findByOrderId(UUID orderId) {
        return repository.findByMaintenanceOrderIdOrderByRegisteredAtAsc(orderId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
