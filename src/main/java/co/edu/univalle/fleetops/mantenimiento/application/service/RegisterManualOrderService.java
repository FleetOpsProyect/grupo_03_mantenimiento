package co.edu.univalle.fleetops.mantenimiento.application.service;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.VehicleSyncException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.RegisterManualOrderUseCase;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OrderRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RegisterManualOrderService implements RegisterManualOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final VehicleServiceClientPort vehicleServiceClientPort;

    public RegisterManualOrderService(OrderRepositoryPort orderRepositoryPort,
                                      VehicleServiceClientPort vehicleServiceClientPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.vehicleServiceClientPort = vehicleServiceClientPort;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceOrder registerManualOrder(MaintenanceOrder order) {
        if (order.getVehicleId() == null) {
            throw new IllegalArgumentException("El vehiculo_id es obligatorio");
        }

        // 1. Consultar sincronamente al microservicio de Vehiculos
        Optional<VehicleServiceClientPort.VehicleSummary> vehicleOpt =
                vehicleServiceClientPort.getVehicleById(order.getVehicleId());

        if (vehicleOpt.isEmpty()) {
            throw new EntityNotFoundException("El vehículo especificado no existe en el sistema");
        }

        VehicleServiceClientPort.VehicleSummary vehicle = vehicleOpt.get();
        if (vehicle.mileage() != null) {
            order.setCurrentMileage(vehicle.mileage());
        }

        // 2. Asignar estado inicial PENDIENTE (SCHEDULED_PENDING) y origen MANUAL
        order.setStatus(MaintenanceStatus.SCHEDULED_PENDING);
        order.setOrigin(MaintenanceOrigin.MANUAL);
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());

        // 3. Persistir la orden de mantenimiento
        MaintenanceOrder savedOrder = orderRepositoryPort.save(order);

        // 4. Notificar a Vehiculos para cambiar su estado a EN_MANTENIMIENTO
        boolean vehicleUpdated = vehicleServiceClientPort.updateVehicleStatus(
                order.getVehicleId(),
                "EN_MANTENIMIENTO"
        );

        if (!vehicleUpdated) {
            // Provoca rollback inmediato de la transaccion de base de datos
            throw new VehicleSyncException("Error de sincronización con el servicio de vehículos");
        }

        return savedOrder;
    }
}
