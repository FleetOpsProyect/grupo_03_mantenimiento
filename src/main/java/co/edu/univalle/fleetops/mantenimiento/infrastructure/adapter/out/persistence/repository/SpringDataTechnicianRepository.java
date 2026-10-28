package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository;

import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTechnicianRepository extends JpaRepository<TechnicianJpaEntity, UUID> {

    Optional<TechnicianJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<TechnicianJpaEntity> findByIsActive(Boolean isActive, Pageable pageable);
}
