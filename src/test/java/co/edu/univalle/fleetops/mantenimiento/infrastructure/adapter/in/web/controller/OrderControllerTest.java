package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.controller;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.VehicleSyncException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceOrder;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceOrigin;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceStatus;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.MaintenanceType;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.RegisterManualOrderUseCase;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderController.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterManualOrderUseCase registerManualOrderUseCase;

    @Test
    @DisplayName("Debe responder 201 Created al registrar exitosamente una orden manual válida en POST /mantenimientos")
    void shouldReturn201WhenManualOrderPayloadIsValid() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        MaintenanceOrder createdOrder = MaintenanceOrder.builder()
                .id(orderId)
                .vehicleId(vehicleId)
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .origin(MaintenanceOrigin.MANUAL)
                .description("Revisión extraordinaria del sistema de frenos")
                .assignedWorkshop("Taller Central")
                .createdAt(LocalDateTime.now())
                .build();

        when(registerManualOrderUseCase.registerManualOrder(any(MaintenanceOrder.class))).thenReturn(createdOrder);

        String payload = """
                {
                  "vehiculo_id": "%s",
                  "tipo_mantenimiento": "PREVENTIVO",
                  "descripcion": "Revisión extraordinaria del sistema de frenos",
                  "taller_asignado": "Taller Central"
                }
                """.formatted(vehicleId);

        mockMvc.perform(post("/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden_id", is(orderId.toString())))
                .andExpect(jsonPath("$.mensaje", is("Orden de mantenimiento creada exitosamente")));
    }

    @Test
    @DisplayName("Debe responder 400 Bad Request cuando faltan campos requeridos como vehiculo_id o descripcion")
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        String payload = """
                {
                  "tipo_mantenimiento": "CORRECTIVO",
                  "taller_asignado": "Taller Central"
                }
                """;

        mockMvc.perform(post("/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", is("Estructura de solicitud inválida o campos requeridos ausentes")));
    }

    @Test
    @DisplayName("Debe responder 400 Bad Request cuando el tipo_mantenimiento es inválido o no contemplado")
    void shouldReturn400WhenMaintenanceTypeIsInvalid() throws Exception {
        String payload = """
                {
                  "vehiculo_id": "%s",
                  "tipo_mantenimiento": "TIPO_INVALIDO",
                  "descripcion": "Revisión técnica de prueba",
                  "taller_asignado": "Taller Central"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", is("Estructura de solicitud inválida o campos requeridos ausentes")));
    }

    @Test
    @DisplayName("Debe responder 400 Bad Request cuando la descripción tiene menos de 10 caracteres")
    void shouldReturn400WhenDescriptionIsTooShort() throws Exception {
        String payload = """
                {
                  "vehiculo_id": "%s",
                  "tipo_mantenimiento": "PREVENTIVO",
                  "descripcion": "Corta",
                  "taller_asignado": "Taller Central"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", is("Estructura de solicitud inválida o campos requeridos ausentes")));
    }

    @Test
    @DisplayName("Debe responder 404 Not Found cuando el vehículo especificado no existe en el sistema")
    void shouldReturn404WhenVehicleDoesNotExist() throws Exception {
        UUID vehicleId = UUID.randomUUID();

        when(registerManualOrderUseCase.registerManualOrder(any(MaintenanceOrder.class)))
                .thenThrow(new EntityNotFoundException("El vehículo especificado no existe en el sistema"));

        String payload = """
                {
                  "vehiculo_id": "%s",
                  "tipo_mantenimiento": "CORRECTIVO",
                  "descripcion": "Cambio de amortiguadores delanteros",
                  "taller_asignado": "Taller Norte"
                }
                """.formatted(vehicleId);

        mockMvc.perform(post("/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", is("El vehículo especificado no existe en el sistema")));
    }

    @Test
    @DisplayName("Debe responder 502 Bad Gateway cuando falla la sincronización con el servicio de vehículos")
    void shouldReturn502WhenVehicleSyncFails() throws Exception {
        UUID vehicleId = UUID.randomUUID();

        when(registerManualOrderUseCase.registerManualOrder(any(MaintenanceOrder.class)))
                .thenThrow(new VehicleSyncException("Error de sincronización con el servicio de vehículos"));

        String payload = """
                {
                  "vehiculo_id": "%s",
                  "tipo_mantenimiento": "PREVENTIVO",
                  "descripcion": "Revisión general de fluidos y frenos",
                  "taller_asignado": "Taller Sur"
                }
                """.formatted(vehicleId);

        mockMvc.perform(post("/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensaje", is("Error de sincronización con el servicio de vehículos")));
    }

    @Test
    @DisplayName("Debe responder 201 Created usando la ruta alternativa en inglés /maintenance")
    void shouldSupportMaintenanceAliasEndpoint() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        MaintenanceOrder createdOrder = MaintenanceOrder.builder()
                .id(orderId)
                .vehicleId(vehicleId)
                .maintenanceType(MaintenanceType.PREVENTIVE)
                .status(MaintenanceStatus.SCHEDULED_PENDING)
                .origin(MaintenanceOrigin.MANUAL)
                .description("Revisión extraordinaria del sistema de frenos")
                .assignedWorkshop("Taller Central")
                .createdAt(LocalDateTime.now())
                .build();

        when(registerManualOrderUseCase.registerManualOrder(any(MaintenanceOrder.class))).thenReturn(createdOrder);

        String payload = """
                {
                  "vehiculo_id": "%s",
                  "tipo_mantenimiento": "PREVENTIVO",
                  "descripcion": "Revisión extraordinaria del sistema de frenos",
                  "taller_asignado": "Taller Central"
                }
                """.formatted(vehicleId);

        mockMvc.perform(post("/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden_id", is(orderId.toString())))
                .andExpect(jsonPath("$.mensaje", is("Orden de mantenimiento creada exitosamente")));
    }
}
