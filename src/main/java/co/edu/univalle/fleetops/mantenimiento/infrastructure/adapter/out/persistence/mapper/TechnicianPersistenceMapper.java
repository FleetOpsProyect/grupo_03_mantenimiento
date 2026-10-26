package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.Technician;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.TechnicianJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TechnicianPersistenceMapper {

    public Technician toDomain(TechnicianJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Technician.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .phoneNumber(entity.getPhoneNumber())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TechnicianJpaEntity toEntity(Technician domain) {
        if (domain == null) {
            return null;
        }
        return TechnicianJpaEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .name(domain.getName())
                .phoneNumber(domain.getPhoneNumber())
                .isActive(domain.getIsActive() != null ? domain.getIsActive() : true)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
