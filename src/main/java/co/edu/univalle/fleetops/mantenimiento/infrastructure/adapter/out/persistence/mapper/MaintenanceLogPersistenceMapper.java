package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLog;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceLogAttachment;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.LogType;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogAttachmentJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class MaintenanceLogPersistenceMapper {

    public MaintenanceLog toDomain(MaintenanceLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return MaintenanceLog.builder()
                .id(entity.getId())
                .orderId(entity.getMaintenanceOrderId())
                .logType(entity.getLogType() != null ? LogType.valueOf(entity.getLogType()) : null)
                .notes(entity.getNotes())
                .costDelta(entity.getCostDelta())
                .authorType(entity.getAuthorType())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthorName())
                .registeredAt(entity.getRegisteredAt())
                .attachments(entity.getAttachments() != null ?
                        entity.getAttachments().stream().map(this::toAttachmentDomain).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }

    public MaintenanceLogJpaEntity toEntity(MaintenanceLog domain) {
        if (domain == null) {
            return null;
        }
        return MaintenanceLogJpaEntity.builder()
                .id(domain.getId())
                .maintenanceOrderId(domain.getOrderId())
                .logType(domain.getLogType() != null ? domain.getLogType().name() : null)
                .notes(domain.getNotes())
                .costDelta(domain.getCostDelta())
                .authorType(domain.getAuthorType())
                .authorId(domain.getAuthorId())
                .authorName(domain.getAuthorName())
                .registeredAt(domain.getRegisteredAt())
                .build();
    }

    public MaintenanceLogAttachment toAttachmentDomain(MaintenanceLogAttachmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return MaintenanceLogAttachment.builder()
                .id(entity.getId())
                .maintenanceLogId(entity.getMaintenanceLogId())
                .fileUrl(entity.getFileUrl())
                .fileName(entity.getFileName())
                .mimeType(entity.getMimeType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MaintenanceLogAttachmentJpaEntity toAttachmentEntity(MaintenanceLogAttachment domain) {
        if (domain == null) {
            return null;
        }
        return MaintenanceLogAttachmentJpaEntity.builder()
                .id(domain.getId())
                .maintenanceLogId(domain.getMaintenanceLogId())
                .fileUrl(domain.getFileUrl())
                .fileName(domain.getFileName())
                .mimeType(domain.getMimeType())
                .fileSizeBytes(domain.getFileSizeBytes())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
