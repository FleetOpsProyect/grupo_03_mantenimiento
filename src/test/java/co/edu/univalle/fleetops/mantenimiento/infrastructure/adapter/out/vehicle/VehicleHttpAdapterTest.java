package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.vehicle;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort.VehicleSummary;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Simula MS Vehículos con MockRestServiceServer: no se abre ninguna conexión real. */
class VehicleHttpAdapterTest {

    private static final String BASE = "http://vehiculos";

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private VehicleHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        circuitBreaker = CircuitBreaker.ofDefaults("test");
        adapter = new VehicleHttpAdapter(builder.build(), circuitBreaker);
    }

    private static String vehicleJson(Object id, String vehicleType, double odometerKm) {
        return "{\"vehicle_id\":\"" + id + "\",\"license_plate\":\"ABC123\",\"vehicle_type\":\"" + vehicleType + "\","
                + "\"status\":\"ACTIVE\",\"odometer_km\":" + odometerKm + "}";
    }

    @Test
    @DisplayName("GET /vehicles/{id} 200: devuelve kilometraje, estado y categoría con contrato en inglés")
    void shouldReturnVehicle() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE + "/vehicles/" + id)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(vehicleJson(id, "PICKUP_TRUCK", 42000.75), MediaType.APPLICATION_JSON));

        Optional<VehicleSummary> result = adapter.getVehicleById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().id());
        assertEquals(42000, result.get().mileage());
        assertEquals("ACTIVE", result.get().status());
        assertTrue(result.get().isActive());
        assertEquals(VehicleCategory.CAMIONETA, result.get().category());
        server.verify();
    }

    @Test
    @DisplayName("GET /vehicles/{id}: mapea CAR a AUTOMOVIL y SPECIALIZED_VEHICLE a CARGA_PESADA")
    void shouldMapCategoriesCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        server.expect(requestTo(BASE + "/vehicles/" + id1))
                .andRespond(withSuccess(vehicleJson(id1, "CAR", 10000.0), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/vehicles/" + id2))
                .andRespond(withSuccess(vehicleJson(id2, "SPECIALIZED_VEHICLE", 20000.0), MediaType.APPLICATION_JSON));

        assertEquals(VehicleCategory.AUTOMOVIL, adapter.getVehicleById(id1).orElseThrow().category());
        assertEquals(VehicleCategory.CARGA_PESADA, adapter.getVehicleById(id2).orElseThrow().category());
        server.verify();
    }

    @Test
    @DisplayName("GET /vehicles/{id} 404: el vehículo no existe")
    void shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE + "/vehicles/" + id)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.getVehicleById(id).isEmpty());
        assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    @DisplayName("GET /vehicles/{id} 5xx: lanza ServiceUnavailableException y cuenta como fallo")
    void shouldThrowWhenServerError() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE + "/vehicles/" + id)).andRespond(withServerError());

        ServiceUnavailableException ex = assertThrows(ServiceUnavailableException.class,
                () -> adapter.getVehicleById(id));
        assertEquals("VEHICLE_SERVICE_UNAVAILABLE", ex.getErrorCode());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    @DisplayName("Circuit Breaker abierto: responde 503 sin llamar a Vehículos")
    void shouldFailFastWhenCircuitOpen() {
        circuitBreaker.transitionToOpenState();

        assertThrows(ServiceUnavailableException.class, () -> adapter.getVehicleById(UUID.randomUUID()));
        server.verify();
    }

    @Test
    @DisplayName("Tipo de vehículo desconocido o ausente: categoría nula")
    void shouldMapUnknownCategoryToNull() {
        UUID id = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        server.expect(requestTo(BASE + "/vehicles/" + id))
                .andRespond(withSuccess("{\"id\":\"" + id + "\",\"vehicle_type\":\"MOTORCYCLE\",\"odometer_km\":1.0}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/vehicles/" + id2))
                .andRespond(withSuccess("{\"id\":\"" + id2 + "\"}", MediaType.APPLICATION_JSON));

        assertNull(adapter.getVehicleById(id).orElseThrow().category());
        assertNull(adapter.getVehicleById(id2).orElseThrow().category());
    }

    @Test
    @DisplayName("GET /vehicles?status=ACTIVE: recorre páginas leyendo content e ignora ids inválidos")
    void shouldFetchAllActiveVehiclePages() {
        String page0 = IntStream.range(0, 100)
                .mapToObj(i -> vehicleJson(i == 0 ? "veh-invalid" : UUID.randomUUID(), "CAR", i))
                .collect(Collectors.joining(",", "{\"content\":[", "],\"page\":0,\"size\":100,\"total\":101}"));
        String page1 = "{\"content\":[" + vehicleJson(UUID.randomUUID(), "SPECIALIZED_VEHICLE", 999)
                + "],\"page\":1,\"size\":100,\"total\":101}";

        server.expect(requestTo(BASE + "/vehicles?status=ACTIVE&page=0&size=100"))
                .andRespond(withSuccess(page0, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/vehicles?status=ACTIVE&page=1&size=100"))
                .andRespond(withSuccess(page1, MediaType.APPLICATION_JSON));

        List<VehicleSummary> vehicles = adapter.getActiveVehicles();

        assertEquals(100, vehicles.size());
        assertEquals(VehicleCategory.CARGA_PESADA, vehicles.get(99).category());
        server.verify();
    }

    @Test
    @DisplayName("GET /vehicles?status=ACTIVE con lista vacía o sin cuerpo")
    void shouldHandleEmptyFleet() {
        server.expect(requestTo(BASE + "/vehicles?status=ACTIVE&page=0&size=100"))
                .andRespond(withSuccess("{\"content\":[],\"total\":0}", MediaType.APPLICATION_JSON));
        assertTrue(adapter.getActiveVehicles().isEmpty());

        server.reset();
        server.expect(requestTo(BASE + "/vehicles?status=ACTIVE&page=0&size=100"))
                .andRespond(withSuccess());
        assertTrue(adapter.getActiveVehicles().isEmpty());
    }

    @Test
    @DisplayName("PUT /vehicles/{id}/status: actualiza estado con maintenance_id normalizando a IN_MAINTENANCE")
    void shouldUpdateVehicleStatus() {
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        server.expect(requestTo(BASE + "/vehicles/" + vehicleId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"status\":\"IN_MAINTENANCE\",\"maintenance_id\":\"" + maintenanceId + "\"}"))
                .andRespond(withSuccess());
        assertTrue(adapter.updateVehicleStatus(vehicleId, maintenanceId, "EN_MANTENIMIENTO"));

        server.reset();
        server.expect(requestTo(BASE + "/vehicles/" + vehicleId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"status\":\"IN_MAINTENANCE\",\"maintenance_id\":\"" + maintenanceId + "\"}"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        assertFalse(adapter.updateVehicleStatus(vehicleId, maintenanceId, "IN_MAINTENANCE"));

        server.reset();
        server.expect(requestTo(BASE + "/vehicles/" + vehicleId + "/status"))
                .andRespond(withServerError());
        assertFalse(adapter.updateVehicleStatus(vehicleId, maintenanceId, "IN_MAINTENANCE"));
    }

    @Test
    @DisplayName("El constructor de Spring configura base URL, timeout y Circuit Breaker")
    void shouldBuildWithSpringConstructor() {
        VehicleHttpAdapter built = new VehicleHttpAdapter(
                RestClient.builder(), CircuitBreakerRegistry.ofDefaults(), BASE, 5000);
        assertNotNull(built);
    }
}
