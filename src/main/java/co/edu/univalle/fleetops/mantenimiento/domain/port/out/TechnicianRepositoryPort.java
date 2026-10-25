package co.edu.univalle.fleetops.mantenimiento.domain.port.out;

import co.edu.univalle.fleetops.mantenimiento.domain.model.Technician;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianRepositoryPort {
    Technician save(Technician technician);
    Optional<Technician> findById(UUID id);
    Optional<Technician> findByEmail(String email);
    List<Technician> findAll(Boolean isActive, int page, int size);
    boolean existsByEmail(String email);
}
