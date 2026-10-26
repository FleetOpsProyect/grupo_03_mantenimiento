package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.adapter;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.OrderFilter;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.QueryOrdersUseCase.PagedOrdersResult;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OrderRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.entity.MaintenanceOrderJpaEntity;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.mapper.MaintenanceOrderPersistenceMapper;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.persistence.repository.SpringDataMaintenanceOrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final SpringDataMaintenanceOrderRepository repository;
    private final MaintenanceOrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(SpringDataMaintenanceOrderRepository repository, MaintenanceOrderPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MaintenanceOrder save(MaintenanceOrder order) {
        if (order.getId() == null) {
            order.setId(UUID.randomUUID());
        }
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());

        MaintenanceOrderJpaEntity entity = mapper.toEntity(order);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceOrder> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceOrder> findActiveOrderByVehicleId(UUID vehicleId) {
        return repository.findFirstByVehicleIdAndStatusIn(
                vehicleId,
                Set.of(MaintenanceStatus.SCHEDULED_PENDING.name(), MaintenanceStatus.IN_PROGRESS.name())
        ).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceOrder> findActiveOrderByVehicleIdAndType(UUID vehicleId, MaintenanceType maintenanceType) {
        if (maintenanceType == null) {
            return findActiveOrderByVehicleId(vehicleId);
        }
        return repository.findFirstByVehicleIdAndMaintenanceTypeAndStatusInOrderByCreatedAtDesc(
                vehicleId,
                maintenanceType.name(),
                Set.of(MaintenanceStatus.SCHEDULED_PENDING.name(), MaintenanceStatus.IN_PROGRESS.name())
        ).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MaintenanceOrder> findLatestCompletedOrderByVehicleId(UUID vehicleId) {
        return repository.findFirstByVehicleIdAndStatusOrderByCompletedAtDesc(
                vehicleId,
                MaintenanceStatus.COMPLETED.name()
        ).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIncidentId(UUID incidentId) {
        return repository.existsByIncidentId(incidentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedOrdersResult queryOrders(OrderFilter filter) {
        Specification<MaintenanceOrderJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status().name()));
            }
            if (filter.maintenanceType() != null) {
                predicates.add(cb.equal(root.get("maintenanceType"), filter.maintenanceType().name()));
            }
            if (filter.vehicleId() != null) {
                predicates.add(cb.equal(root.get("vehicleId"), filter.vehicleId()));
            }
            if (filter.technicianId() != null) {
                predicates.add(cb.equal(root.get("technicianId"), filter.technicianId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int pageNumber = Math.max(0, filter.page() > 0 ? filter.page() - 1 : 0);
        int pageSize = Math.min(100, Math.max(1, filter.size()));
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<MaintenanceOrderJpaEntity> page = repository.findAll(spec, pageRequest);
        List<MaintenanceOrder> content = page.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        Double avgHours = repository.calculateAverageRepairHoursFiltered(
                filter.vehicleId(),
                filter.technicianId(),
                filter.maintenanceType() != null ? filter.maintenanceType().name() : null
        );
        summary.put("avg_repair_hours", avgHours != null ? Math.round(avgHours * 100.0) / 100.0 : null);

        return new PagedOrdersResult(
                content,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                summary
        );
    }
}
