package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.client;

import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleHttpAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private VehicleHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new VehicleHttpAdapter(restTemplate, "http://localhost:8081");
    }

    @Test
    @DisplayName("Debe consultar vehículo exitosamente y mapear sus campos")
    void shouldReturnVehicleSummaryWhenVehicleExists() {
        UUID vehicleId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("id", vehicleId.toString());
        body.put("kilometraje_actual", 34500);
        body.put("estado", "ACTIVO");
        body.put("categoria", "CAMIONETA");

        when(restTemplate.getForEntity("http://localhost:8081/vehiculos/" + vehicleId, Map.class))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        Optional<VehicleServiceClientPort.VehicleSummary> result = adapter.getVehicleById(vehicleId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(vehicleId);
        assertThat(result.get().mileage()).isEqualTo(34500);
        assertThat(result.get().status()).isEqualTo("ACTIVO");
        assertThat(result.get().category()).isEqualTo(VehicleCategory.CAMIONETA);
    }

    @Test
    @DisplayName("Debe retornar Optional.empty() cuando el servicio de vehículos responde 404 Not Found")
    void shouldReturnEmptyOptionalWhenVehicleNotFound404() {
        UUID vehicleId = UUID.randomUUID();

        when(restTemplate.getForEntity("http://localhost:8081/vehiculos/" + vehicleId, Map.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        Optional<VehicleServiceClientPort.VehicleSummary> result = adapter.getVehicleById(vehicleId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar true cuando la actualización del estado del vehículo responde exitoso")
    void shouldReturnTrueWhenUpdateVehicleStatusSucceeds() {
        UUID vehicleId = UUID.randomUUID();

        when(restTemplate.exchange(
                eq("http://localhost:8081/vehiculos/" + vehicleId + "/estado"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean updated = adapter.updateVehicleStatus(vehicleId, UUID.randomUUID(), "EN_MANTENIMIENTO");

        assertThat(updated).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false cuando la comunicación falla con ResourceAccessException (falla de red)")
    void shouldReturnFalseWhenUpdateVehicleStatusFailsNetwork() {
        UUID vehicleId = UUID.randomUUID();

        when(restTemplate.exchange(
                eq("http://localhost:8081/vehiculos/" + vehicleId + "/estado"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        boolean updated = adapter.updateVehicleStatus(vehicleId, UUID.randomUUID(), "EN_MANTENIMIENTO");

        assertThat(updated).isFalse();
    }
}
