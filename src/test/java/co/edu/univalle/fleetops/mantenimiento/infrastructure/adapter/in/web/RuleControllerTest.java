package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.BusinessRuleException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.EntityNotFoundException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ResourceConflictException;
import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.MaintenanceRule;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.RuleScope;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.in.RegisterRuleUseCase;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.in.web.mapper.RuleWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba solo la capa web: Spring levanta el controller, la validación, Jackson y el
 * manejador global de errores, pero el caso de uso es un mock (no hay BD ni Vehículos).
 */
@WebMvcTest(RuleController.class)
@Import(RuleWebMapper.class)
class RuleControllerTest {

    private static final String URL = "/maintenance/rules";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterRuleUseCase registerRuleUseCase;

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @DisplayName("201: registra una regla CATEGORIA y responde en snake_case con valores del contrato")
    void shouldRegisterRule() throws Exception {
        UUID id = UUID.randomUUID();
        when(registerRuleUseCase.registerRule(any())).thenAnswer(inv -> {
            MaintenanceRule rule = inv.getArgument(0);
            rule.setId(id);
            rule.setIsActive(true);
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
            return rule;
        });

        postJson("""
                {"scope_type":"CATEGORIA","vehicle_category":"carga_pesada",
                 "mileage_interval_km":8000,"time_interval_days":90,"routine_description":"Cambio de aceite"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.scope_type").value("CATEGORIA"))
                .andExpect(jsonPath("$.vehicle_category").value("CARGA_PESADA"))
                .andExpect(jsonPath("$.mileage_interval_km").value(8000))
                .andExpect(jsonPath("$.time_interval_days").value(90))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.created_at").exists());

        ArgumentCaptor<MaintenanceRule> captor = ArgumentCaptor.forClass(MaintenanceRule.class);
        verify(registerRuleUseCase).registerRule(captor.capture());
        assertEquals(RuleScope.CATEGORY, captor.getValue().getScope());
        assertEquals(VehicleCategory.CARGA_PESADA, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("201: acepta también los valores en inglés y responde VEHICULO")
    void shouldAcceptEnglishScopeValues() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        when(registerRuleUseCase.registerRule(any())).thenAnswer(inv -> inv.getArgument(0));

        postJson("{\"scope_type\":\"VEHICLE\",\"vehicle_id\":\"" + vehicleId + "\","
                + "\"mileage_interval_km\":5000,\"time_interval_days\":60}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope_type").value("VEHICULO"))
                .andExpect(jsonPath("$.vehicle_id").value(vehicleId.toString()));
    }

    @Test
    @DisplayName("400: faltan campos obligatorios; lista todos los campos inválidos")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        postJson("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.ruta").value(URL))
                .andExpect(jsonPath("$.errores.length()").value(3))
                .andExpect(jsonPath("$.errores[?(@.campo == 'scope_type')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'mileage_interval_km')]").exists());

        verify(registerRuleUseCase, never()).registerRule(any());
    }

    @Test
    @DisplayName("400: scope_type fuera del catálogo")
    void shouldReturn400WhenScopeInvalid() throws Exception {
        postJson("{\"scope_type\":\"FLOTA\",\"mileage_interval_km\":5000,\"time_interval_days\":60}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("scope_type"))
                .andExpect(jsonPath("$.errores[0].codigo").value("INVALID_VALUE"));
    }

    @Test
    @DisplayName("400: vehicle_category fuera del catálogo")
    void shouldReturn400WhenCategoryInvalid() throws Exception {
        postJson("{\"scope_type\":\"CATEGORIA\",\"vehicle_category\":\"MOTO\","
                + "\"mileage_interval_km\":5000,\"time_interval_days\":60}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("vehicle_category"));
    }

    @Test
    @DisplayName("400: vehicle_id no es un UUID o el JSON está mal formado")
    void shouldReturn400WhenBodyUnreadable() throws Exception {
        postJson("{\"scope_type\":\"VEHICULO\",\"vehicle_id\":\"123-abc\","
                + "\"mileage_interval_km\":5000,\"time_interval_days\":60}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MALFORMED_REQUEST"));

        postJson("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("422: el dominio rechaza umbrales fuera de rango")
    void shouldReturn422OnBusinessRule() throws Exception {
        when(registerRuleUseCase.registerRule(any()))
                .thenThrow(new BusinessRuleException("INVALID_KM_THRESHOLD", "El umbral de kilometraje..."));

        postJson("{\"scope_type\":\"GLOBAL\",\"mileage_interval_km\":10,\"time_interval_days\":60}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("INVALID_KM_THRESHOLD"))
                .andExpect(jsonPath("$.errores").doesNotExist());
    }

    @Test
    @DisplayName("409: regla activa duplicada (verificación previa o índice único de la BD)")
    void shouldReturn409OnConflict() throws Exception {
        String body = "{\"scope_type\":\"GLOBAL\",\"mileage_interval_km\":10000,\"time_interval_days\":180}";

        when(registerRuleUseCase.registerRule(any()))
                .thenThrow(new ResourceConflictException("RULE_ALREADY_EXISTS", "Ya existe una regla activa"));
        postJson(body).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RULE_ALREADY_EXISTS"));

        doThrow(new DataIntegrityViolationException("uq_active_global_rule")).when(registerRuleUseCase).registerRule(any());
        postJson(body).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RESOURCE_CONFLICT"));
    }

    @Test
    @DisplayName("404: el vehículo de la regla no existe")
    void shouldReturn404WhenVehicleNotFound() throws Exception {
        when(registerRuleUseCase.registerRule(any()))
                .thenThrow(new EntityNotFoundException("VEHICLE_NOT_FOUND", "El vehículo especificado no existe"));

        postJson("{\"scope_type\":\"VEHICULO\",\"vehicle_id\":\"" + UUID.randomUUID() + "\","
                + "\"mileage_interval_km\":5000,\"time_interval_days\":60}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    @DisplayName("503: Vehículos o la base de datos no están disponibles")
    void shouldReturn503WhenDependencyDown() throws Exception {
        String body = "{\"scope_type\":\"GLOBAL\",\"mileage_interval_km\":10000,\"time_interval_days\":180}";

        when(registerRuleUseCase.registerRule(any()))
                .thenThrow(new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE", "Vehículos caído"));
        postJson(body).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("VEHICLE_SERVICE_UNAVAILABLE"));

        doThrow(new QueryTimeoutException("db down")).when(registerRuleUseCase).registerRule(any());
        postJson(body).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio no disponible temporalmente, intente más tarde"));
    }

    @Test
    @DisplayName("500: error inesperado sin exponer detalles internos")
    void shouldReturn500OnUnexpectedError() throws Exception {
        when(registerRuleUseCase.registerRule(any())).thenThrow(new IllegalStateException("detalle interno"));

        postJson("{\"scope_type\":\"GLOBAL\",\"mileage_interval_km\":10000,\"time_interval_days\":180}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }

    @Test
    @DisplayName("405: método no soportado conserva su código HTTP")
    void shouldKeepSpringMvcStatusCodes() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("HTTP_405"));
    }
}
