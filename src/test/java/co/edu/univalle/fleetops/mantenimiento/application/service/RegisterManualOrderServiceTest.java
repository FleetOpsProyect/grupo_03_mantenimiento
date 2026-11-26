package co.edu.univalle.fleetops.mantenimiento.application.service;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.VehicleSyncException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.OrderRepositoryPort;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterManualOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private VehicleServiceClientPort vehicleServiceClientPort;

    @InjectMocks
    private RegisterManualOrderService service;

    @Test
    @DisplayName("Debe registrar exitosamente una orden manual preventiva cuando el vehículo existe y se sincroniza")
    void shouldRegisterManualOrderSuccessfullyWithPreventive() {
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();

        MaintenanceOrder inputOrder = MaintenanceOrder.builder()
                .vehicleId(vehicleId)
                .technicianId(technicianId)
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .description("Revisión de frenos programada manualmente")
                .assignedWorkshop("Taller Principal")
                .build();

        VehicleServiceClientPort.VehicleSummary vehicleSummary =
                new VehicleServiceClientPort.VehicleSummary(vehicleId, 25000, "ACTIVO", VehicleCategory.CAMIONETA);

        when(vehicleServiceClientPort.getVehicleById(vehicleId)).thenReturn(Optional.of(vehicleSummary));
        when(orderRepositoryPort.save(any(MaintenanceOrder.class))).thenAnswer(invocation -> {
            MaintenanceOrder saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(vehicleServiceClientPort.updateVehicleStatus(eq(vehicleId), any(UUID.class), eq("EN_MANTENIMIENTO"))).thenReturn(true);

        MaintenanceOrder result = service.registerManualOrder(inputOrder);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MaintenanceStatus.SCHEDULED_PENDING);
        assertThat(result.getOrigin()).isEqualTo(MaintenanceOrigin.MANUAL);
        assertThat(result.getCurrentMileage()).isEqualTo(25000);
        assertThat(result.getAssignedWorkshop()).isEqualTo("Taller Principal");

        verify(vehicleServiceClientPort).getVehicleById(vehicleId);
        verify(orderRepositoryPort).save(any(MaintenanceOrder.class));
        verify(vehicleServiceClientPort).updateVehicleStatus(eq(vehicleId), any(UUID.class), eq("EN_MANTENIMIENTO"));
    }

    @Test
    @DisplayName("Debe rechazar con EntityNotFoundException si el vehículo no existe en el sistema (404)")
    void shouldThrowEntityNotFoundExceptionWhenVehicleDoesNotExist() {
        UUID vehicleId = UUID.randomUUID();

        MaintenanceOrder inputOrder = MaintenanceOrder.builder()
                .vehicleId(vehicleId)
                .maintenanceType(MaintenanceType.CORRECTIVE)
                .description("Reparación de embrague por desgaste")
                .assignedWorkshop("Taller Norte")
                .build();

        when(vehicleServiceClientPort.getVehicleById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerManualOrder(inputOrder))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("El vehículo especificado no existe en el sistema");

        verify(vehicleServiceClientPort).getVehicleById(vehicleId);
        verify(orderRepositoryPort, never()).save(any());
        verify(vehicleServiceClientPort, never()).updateVehicleStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar VehicleSyncException para rollback si la actualización de estado en Vehículos falla")
    void shouldThrowVehicleSyncExceptionWhenVehicleStatusUpdateFails() {
        UUID vehicleId = UUID.randomUUID();

        MaintenanceOrder inputOrder = MaintenanceOrder.builder()
                .vehicleId(vehicleId)
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .description("Mantenimiento general de 10000 km")
                .assignedWorkshop("Taller Sur")
                .build();

        VehicleServiceClientPort.VehicleSummary vehicleSummary =
                new VehicleServiceClientPort.VehicleSummary(vehicleId, 10000, "ACTIVO", VehicleCategory.AUTOMOVIL);

        when(vehicleServiceClientPort.getVehicleById(vehicleId)).thenReturn(Optional.of(vehicleSummary));
        when(orderRepositoryPort.save(any(MaintenanceOrder.class))).thenAnswer(invocation -> {
            MaintenanceOrder saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(vehicleServiceClientPort.updateVehicleStatus(eq(vehicleId), any(UUID.class), eq("EN_MANTENIMIENTO"))).thenReturn(false);

        assertThatThrownBy(() -> service.registerManualOrder(inputOrder))
                .isInstanceOf(VehicleSyncException.class)
                .hasMessage("Error de sincronización con el servicio de vehículos");

        verify(orderRepositoryPort).save(any(MaintenanceOrder.class));
        verify(vehicleServiceClientPort).updateVehicleStatus(eq(vehicleId), any(UUID.class), eq("EN_MANTENIMIENTO"));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si no se especifica el vehicleId")
    void shouldThrowIllegalArgumentExceptionWhenVehicleIdIsNull() {
        MaintenanceOrder inputOrder = MaintenanceOrder.builder()
                .description("Orden sin vehiculo")
                .build();

        assertThatThrownBy(() -> service.registerManualOrder(inputOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El vehiculo_id es obligatorio");

        verify(vehicleServiceClientPort, never()).getVehicleById(any());
    }
}
