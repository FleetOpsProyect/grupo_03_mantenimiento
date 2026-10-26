package co.edu.univalle.fleetops.mantenimiento.domain.port.in;

import co.edu.univalle.fleetops.mantenimiento.domain.model.Technician;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageTechniciansUseCase {

    Technician registerTechnician(Technician technician);

    List<Technician> listTechnicians(Boolean isActive, int page, int size);

    Optional<Technician> findByEmail(String email);

    Optional<Technician> findById(UUID id);
}
