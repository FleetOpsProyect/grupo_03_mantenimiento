package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.vehicle;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Componente de health "vehicles": consulta activamente a MS Vehículos con una petición paginada liviana.
 *
 * <p>La sonda no pasa por el Circuit Breaker "vehicleService" a propósito, para que los chequeos de
 * salud nunca lo abran; solo informa su estado. El resultado se guarda en caché por un TTL corto
 * para que cada scrape de Prometheus no llegue a MS Vehículos. Este componente queda fuera del grupo
 * readiness: una caída de MS Vehículos no debe marcar este servicio como no listo.
 */
@Component
public class VehiclesHealthIndicator implements HealthIndicator {

    private static final String CIRCUIT_BREAKER_NAME = "vehicleService";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Clock clock;
    private final Duration cacheTtl;

    /** Resultado y momento de la sonda en un solo objeto inmutable: se publica con una única escritura. */
    private volatile CachedProbe cache;

    @Autowired
    public VehiclesHealthIndicator(RestClient.Builder builder,
                                   CircuitBreakerRegistry circuitBreakerRegistry,
                                   @Value("${services.vehicles.url}") String baseUrl,
                                   @Value("${services.vehicles.health.timeout-ms:2000}") long timeoutMs,
                                   @Value("${services.vehicles.health.cache-ttl-ms:30000}") long cacheTtlMs) {
        this(builder.clone().baseUrl(baseUrl).requestFactory(timeoutRequestFactory(timeoutMs)).build(),
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME),
                Clock.systemUTC(),
                Duration.ofMillis(cacheTtlMs));
    }

    /** Constructor usado por las pruebas para inyectar un RestClient simulado y un reloj controlable. */
    VehiclesHealthIndicator(RestClient restClient, CircuitBreaker circuitBreaker, Clock clock, Duration cacheTtl) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public Health health() {
        Instant now = clock.instant();
        CachedProbe current = cache;
        if (current != null && now.isBefore(current.probedAt().plus(cacheTtl))) {
            return current.health();
        }
        Health fresh = probe(now);
        cache = new CachedProbe(fresh, now);
        return fresh;
    }

    private Health probe(Instant start) {
        Health.Builder builder;
        try {
            restClient.get()
                    .uri(uri -> uri.path("/vehicles").queryParam("page", 0).queryParam("size", 1).build())
                    .retrieve()
                    .toBodilessEntity();
            builder = Health.up();
        } catch (RestClientResponseException ex) {
            // Solo el código HTTP: el mensaje de la excepción incluye el cuerpo de la respuesta.
            builder = Health.down().withDetail("error", "HTTP " + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            builder = Health.down().withDetail("error", ex.getClass().getSimpleName());
        }
        return builder
                .withDetail("latencyMs", Duration.between(start, clock.instant()).toMillis())
                .withDetail("circuitBreaker", circuitBreaker.getState().name())
                .build();
    }

    private record CachedProbe(Health health, Instant probedAt) {
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory(long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return factory;
    }
}
