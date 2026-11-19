package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.vehicle;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Adaptador de salida hacia MS Vehículos (HTTP síncrono).
 * Timeout configurable (5 s por defecto) y Circuit Breaker "vehicleService" de Resilience4j:
 * si Vehículos falla repetidamente, se dejan de enviar llamadas y se responde 503 de inmediato.
 */
@Component
public class VehicleHttpAdapter implements VehicleServiceClientPort {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleHttpAdapter.class);
    private static final String CIRCUIT_BREAKER_NAME = "vehicleService";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 100;

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    @Autowired
    public VehicleHttpAdapter(RestClient.Builder builder,
                              CircuitBreakerRegistry circuitBreakerRegistry,
                              @Value("${services.vehicles.url}") String baseUrl,
                              @Value("${services.vehicles.timeout-ms:5000}") long timeoutMs) {
        this(builder.baseUrl(baseUrl).requestFactory(timeoutRequestFactory(timeoutMs)).build(),
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME));
    }

    /** Constructor usado por las pruebas para inyectar un RestClient simulado. */
    VehicleHttpAdapter(RestClient restClient, CircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Optional<VehicleSummary> getVehicleById(UUID vehicleId) {
        return call("consultar el vehículo " + vehicleId, () -> {
            try {
                VehicleDto dto = restClient.get()
                        .uri("/vehiculos/{id}", vehicleId)
                        .retrieve()
                        .body(VehicleDto.class);
                return Optional.ofNullable(dto).map(d -> toSummary(d, vehicleId));
            } catch (HttpClientErrorException.NotFound ex) {
                // 404 es una respuesta de negocio válida, no un fallo de la dependencia
                return Optional.empty();
            }
        });
    }

    @Override
    public List<VehicleSummary> getActiveVehicles() {
        return call("consultar la flota activa", () -> {
            List<VehicleSummary> result = new ArrayList<>();
            long fetched = 0;
            for (int page = 0; page < MAX_PAGES; page++) {
                VehiclePageDto body = fetchActivePage(page);
                List<VehicleDto> content = body == null || body.contenido() == null ? List.of() : body.contenido();
                fetched += content.size();
                for (VehicleDto dto : content) {
                    parseId(dto.id()).ifPresent(id -> result.add(toSummary(dto, id)));
                }
                long total = body != null && body.total() != null ? body.total() : fetched;
                if (content.size() < PAGE_SIZE || fetched >= total) {
                    break;
                }
            }
            return result;
        });
    }

    @Override
    public boolean updateVehicleStatus(UUID vehicleId, String newStatus) {
        try {
            return call("actualizar el estado del vehículo " + vehicleId, () -> {
                try {
                    restClient.put()
                            .uri("/vehiculos/{id}/estado", vehicleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("estado", newStatus))
                            .retrieve()
                            .toBodilessEntity();
                    return true;
                } catch (HttpClientErrorException ex) {
                    // 4xx (p. ej. 409: el vehículo no estaba ACTIVO): Vehículos rechazó el cambio
                    LOG.warn("MS Vehículos rechazó el cambio de estado del vehículo {} a {}: HTTP {}",
                            vehicleId, newStatus, ex.getStatusCode().value());
                    return false;
                }
            });
        } catch (ServiceUnavailableException ex) {
            return false;
        }
    }

    private VehiclePageDto fetchActivePage(int page) {
        return restClient.get()
                .uri(uri -> uri.path("/vehiculos")
                        .queryParam("estado", "ACTIVO")
                        .queryParam("page", page)
                        .queryParam("size", PAGE_SIZE)
                        .build())
                .retrieve()
                .body(VehiclePageDto.class);
    }

    private <T> T call(String operation, Supplier<T> supplier) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (CallNotPermittedException ex) {
            LOG.warn("Circuit Breaker abierto: no se intentó {}", operation);
            throw unavailable();
        } catch (RestClientException ex) {
            // Incluye timeouts (ResourceAccessException) y errores 5xx
            LOG.error("Fallo al {} en MS Vehículos: {}", operation, ex.getMessage());
            throw unavailable();
        }
    }

    private static ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE",
                "El servicio de vehículos no está disponible temporalmente, intente más tarde");
    }

    private static VehicleSummary toSummary(VehicleDto dto, UUID id) {
        return new VehicleSummary(id, dto.kilometrajeActual(), dto.estado(), parseCategory(dto.tipo()));
    }

    private static Optional<UUID> parseId(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException ex) {
            LOG.warn("Vehículo ignorado: id '{}' no es un UUID válido", raw);
            return Optional.empty();
        }
    }

    private static VehicleCategory parseCategory(String tipo) {
        if (tipo == null) {
            return null;
        }
        try {
            return VehicleCategory.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory(long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return factory;
    }

    /** Respuesta de GET /vehiculos/{id} (contrato de MS Vehículos, snake_case). */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record VehicleDto(String id, String estado, String tipo, Integer kilometrajeActual) {
    }

    /** Respuesta paginada de GET /vehiculos: {contenido: [...], page, size, total}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record VehiclePageDto(List<VehicleDto> contenido, Long total) {
    }
}
