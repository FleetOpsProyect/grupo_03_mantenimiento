package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.Technician;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.TechnicianRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.TechnicianPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataTechnicianRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TechnicianPersistenceAdapter implements TechnicianRepositoryPort {

    private final SpringDataTechnicianRepository repository;
    private final TechnicianPersistenceMapper mapper;

    public TechnicianPersistenceAdapter(SpringDataTechnicianRepository repository, TechnicianPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Technician save(Technician technician) {
        if (technician.getId() == null) {
            technician.setId(UUID.randomUUID());
        }
        if (technician.getCreatedAt() == null) {
            technician.setCreatedAt(LocalDateTime.now());
        }
        technician.setUpdatedAt(LocalDateTime.now());

        TechnicianJpaEntity entity = mapper.toEntity(technician);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Technician> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Technician> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<Technician> findAll(Boolean isActive, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        if (isActive != null) {
            return repository.findByIsActive(isActive, pageRequest).map(mapper::toDomain).getContent();
        }
        return repository.findAll(pageRequest).map(mapper::toDomain).getContent();
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
