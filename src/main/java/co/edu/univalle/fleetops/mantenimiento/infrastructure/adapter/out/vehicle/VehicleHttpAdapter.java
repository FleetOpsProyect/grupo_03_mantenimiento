package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.vehicle;

import co.edu.univalle.fleetops.mantenimiento.domain.exception.ServiceUnavailableException;
import co.edu.univalle.fleetops.mantenimiento.domain.model.enums.VehicleCategory;
import co.edu.univalle.fleetops.mantenimiento.domain.port.out.VehicleServiceClientPort;
import com.fasterxml.jackson.annotation.JsonAlias;
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
 * Timeout configurable (5 s por defecto) y Circuit Breaker "vehicleService" de Resilience4j.
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
                        .uri("/vehicles/{id}", vehicleId)
                        .retrieve()
                        .body(VehicleDto.class);
                return Optional.ofNullable(dto).map(d -> toSummary(d, vehicleId));
            } catch (HttpClientErrorException.NotFound ex) {
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
                List<VehicleDto> content = body == null || body.content() == null ? List.of() : body.content();
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
    public boolean updateVehicleStatus(UUID vehicleId, UUID maintenanceId, String newStatus) {
        String normalizedStatus = normalizeStatus(newStatus);
        String maintenanceIdStr = maintenanceId != null ? maintenanceId.toString() : "";
        Map<String, String> payload = Map.of(
                "status", normalizedStatus,
                "maintenance_id", maintenanceIdStr
        );

        try {
            return call("actualizar el estado del vehículo " + vehicleId, () -> {
                try {
                    restClient.put()
                            .uri("/vehicles/{id}/status", vehicleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .toBodilessEntity();
                    return true;
                } catch (HttpClientErrorException ex) {
                    LOG.warn("MS Vehículos rechazó el cambio de estado del vehículo {} a {}: HTTP {}",
                            vehicleId, normalizedStatus, ex.getStatusCode().value());
                    return false;
                }
            });
        } catch (ServiceUnavailableException ex) {
            return false;
        }
    }

    private VehiclePageDto fetchActivePage(int page) {
        return restClient.get()
                .uri(uri -> uri.path("/vehicles")
                        .queryParam("status", "ACTIVE")
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
            LOG.error("Fallo al {} en MS Vehículos: {}", operation, ex.getMessage());
            throw unavailable();
        }
    }

    private static ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("VEHICLE_SERVICE_UNAVAILABLE",
                "El servicio de vehículos no está disponible temporalmente, intente más tarde");
    }

    private static VehicleSummary toSummary(VehicleDto dto, UUID id) {
        Integer mileage = dto.odometerKm() != null ? dto.odometerKm().intValue() : null;
        return new VehicleSummary(id, mileage, dto.status(), parseCategory(dto.vehicleType()));
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
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        String normalized = tipo.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CAR", "AUTOMOVIL" -> VehicleCategory.AUTOMOVIL;
            case "PICKUP_TRUCK", "CAMIONETA" -> VehicleCategory.CAMIONETA;
            case "SPECIALIZED_VEHICLE", "CARGA_PESADA" -> VehicleCategory.CARGA_PESADA;
            default -> {
                try {
                    yield VehicleCategory.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return "IN_MAINTENANCE";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EN_MANTENIMIENTO", "IN_MAINTENANCE" -> "IN_MAINTENANCE";
            case "ACTIVO", "ACTIVE" -> "ACTIVE";
            case "BLOQUEADO", "BLOCKED" -> "BLOCKED";
            default -> normalized;
        };
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory(long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return factory;
    }

    /** Respuesta de GET /vehicles/{id} (contrato oficial de MS Vehículos, snake_case). */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record VehicleDto(
            @JsonAlias("vehicle_id") String id,
            @JsonAlias("estado") String status,
            @JsonAlias("tipo") String vehicleType,
            @JsonAlias("kilometraje_actual") Double odometerKm
    ) {
    }

    /** Respuesta paginada de GET /vehicles: {content: [...], page, size, total}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record VehiclePageDto(
            @JsonAlias("contenido") List<VehicleDto> content,
            Long total
    ) {
    }
}
