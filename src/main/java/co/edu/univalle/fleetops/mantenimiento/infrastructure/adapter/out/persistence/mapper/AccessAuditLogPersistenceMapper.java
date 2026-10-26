package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.AccessAuditLog;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.AccessAuditLogJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AccessAuditLogPersistenceMapper {

    public AccessAuditLog toDomain(AccessAuditLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AccessAuditLog.builder()
                .id(entity.getId())
                .actorType(entity.getActorType())
                .actorId(entity.getActorId())
                .actorEmail(entity.getActorEmail())
                .action(entity.getAction())
                .resourceAccessed(entity.getResourceAccessed())
                .httpMethod(entity.getHttpMethod())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .statusResult(entity.getStatusResult())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AccessAuditLogJpaEntity toEntity(AccessAuditLog domain) {
        if (domain == null) {
            return null;
        }
        return AccessAuditLogJpaEntity.builder()
                .id(domain.getId())
                .actorType(domain.getActorType())
                .actorId(domain.getActorId())
                .actorEmail(domain.getActorEmail())
                .action(domain.getAction())
                .resourceAccessed(domain.getResourceAccessed())
                .httpMethod(domain.getHttpMethod())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .statusResult(domain.getStatusResult())
                .details(domain.getDetails())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
