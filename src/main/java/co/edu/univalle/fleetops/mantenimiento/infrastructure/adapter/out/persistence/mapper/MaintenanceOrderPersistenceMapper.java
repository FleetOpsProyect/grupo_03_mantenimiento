package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceLogJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MaintenanceOrderPersistenceMapper {

    private final MaintenanceLogPersistenceMapper logMapper;

    public MaintenanceOrderPersistenceMapper(MaintenanceLogPersistenceMapper logMapper) {
        this.logMapper = logMapper;
    }

    public MaintenanceOrder toDomain(MaintenanceOrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return MaintenanceOrder.builder()
                .id(entity.getId())
                .vehicleId(entity.getVehicleId())
                .technicianId(entity.getTechnicianId())
                .incidentId(entity.getIncidentId())
                .maintenanceType(entity.getMaintenanceType() != null ? MaintenanceType.valueOf(entity.getMaintenanceType()) : null)
                .status(entity.getStatus() != null ? MaintenanceStatus.valueOf(entity.getStatus()) : null)
                .origin(entity.getOrigin() != null ? MaintenanceOrigin.valueOf(entity.getOrigin()) : null)
                .currentMileage(entity.getCurrentMileage())
                .description(entity.getDescription())
                .reason(entity.getCancelReason())
                .assignedWorkshop(entity.getAssignedWorkshop())
                .completedByType(entity.getCompletedByType())
                .completedById(entity.getCompletedById())
                .totalCost(entity.getTotalCost())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .logs(entity.getLogs() != null
                        ? entity.getLogs().stream().map(logMapper::toDomain).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }

    public MaintenanceOrderJpaEntity toEntity(MaintenanceOrder domain) {
        if (domain == null) {
            return null;
        }

        MaintenanceOrderJpaEntity entity = MaintenanceOrderJpaEntity.builder()
                .id(domain.getId())
                .vehicleId(domain.getVehicleId())
                .technicianId(domain.getTechnicianId())
                .incidentId(domain.getIncidentId())
                .maintenanceType(domain.getMaintenanceType() != null ? domain.getMaintenanceType().name() : null)
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .origin(domain.getOrigin() != null ? domain.getOrigin().name() : null)
                .currentMileage(domain.getCurrentMileage())
                .description(domain.getDescription())
                .cancelReason(domain.getReason())
                .assignedWorkshop(domain.getAssignedWorkshop())
                .completedByType(domain.getCompletedByType())
                .completedById(domain.getCompletedById())
                .totalCost(domain.getTotalCost())
                .completedAt(domain.getCompletedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();

        if (domain.getLogs() != null) {
            List<MaintenanceLogJpaEntity> logEntities = domain.getLogs().stream()
                    .map(logDomain -> {
                        MaintenanceLogJpaEntity logEntity = logMapper.toEntity(logDomain);
                        if (logEntity != null) {
                            logEntity.setOrder(entity);
                        }
                        return logEntity;
                    })
                    .collect(Collectors.toList());
            entity.setLogs(logEntities);
        }

        return entity;
    }
}
