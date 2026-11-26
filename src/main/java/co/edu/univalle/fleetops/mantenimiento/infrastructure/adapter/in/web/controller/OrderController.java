package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.controller;

import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.RegisterManualOrderUseCase;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.CreateManualOrderRequest;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.dto.CreateManualOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Tag(name = "Órdenes de Mantenimiento", description = "Endpoints para la gestión de órdenes de trabajo")
public class OrderController {

    private final RegisterManualOrderUseCase registerManualOrderUseCase;

    public OrderController(RegisterManualOrderUseCase registerManualOrderUseCase) {
        this.registerManualOrderUseCase = registerManualOrderUseCase;
    }

    @PostMapping({"/mantenimientos", "/maintenance"})
    @Operation(summary = "Registrar orden manual extraordinaria (HU-03)")
    public ResponseEntity<CreateManualOrderResponse> createManualOrder(
            @Valid @RequestBody CreateManualOrderRequest request) {

        MaintenanceType type = ("PREVENTIVO".equalsIgnoreCase(request.getTipoMantenimiento())
                || "PREVENTIVE".equalsIgnoreCase(request.getTipoMantenimiento()))
                ? MaintenanceType.PREVENTIVE
                : MaintenanceType.CORRECTIVE;

        MaintenanceOrder order = MaintenanceOrder.builder()
                .vehicleId(request.getVehiculoId())
                .maintenanceType(type)
                .description(request.getDescripcion())
                .assignedWorkshop(request.getTallerAsignado())
                .technicianId(request.getTecnicoId())
                .build();

        MaintenanceOrder createdOrder = registerManualOrderUseCase.registerManualOrder(order);

        CreateManualOrderResponse response = CreateManualOrderResponse.builder()
                .ordenId(createdOrder.getId())
                .mensaje("Orden de mantenimiento creada exitosamente")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
