package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.vehicle;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Simula MS Vehículos con MockRestServiceServer: no se abre ninguna conexión real. */
class VehiclesHealthIndicatorTest {

    private static final String BASE = "http://vehiculos";
    private static final String PROBE_URL = BASE + "/vehicles?page=0&size=1";
    private static final Duration TTL = Duration.ofSeconds(30);

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private MutableClock clock;
    private VehiclesHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        circuitBreaker = CircuitBreaker.ofDefaults("vehicleService");
        clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
        indicator = new VehiclesHealthIndicator(builder.build(), circuitBreaker, clock, TTL);
    }

    @Test
    @DisplayName("Reporta UP con latencia y estado del Circuit Breaker cuando MS Vehículos responde 200")
    void shouldReportUpWhenVehiclesResponds() {
        server.expect(once(), requestTo(PROBE_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"content\":[],\"total\":0}", MediaType.APPLICATION_JSON));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertNotNull(health.getDetails().get("latencyMs"));
        assertEquals("CLOSED", health.getDetails().get("circuitBreaker"));
        server.verify();
    }

    @Test
    @DisplayName("Reporta DOWN con el error cuando MS Vehículos responde 5xx")
    void shouldReportDownOnServerError() {
        server.expect(once(), requestTo(PROBE_URL)).andRespond(withServerError());

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
        assertEquals("CLOSED", health.getDetails().get("circuitBreaker"));
    }

    @Test
    @DisplayName("No expone el cuerpo de la respuesta de error de MS Vehículos, solo el código HTTP")
    void shouldNotLeakResponseBodyInErrorDetail() {
        server.expect(once(), requestTo(PROBE_URL))
                .andRespond(withServerError().body("internal-secret-stacktrace").contentType(MediaType.TEXT_PLAIN));

        Health health = indicator.health();

        String error = String.valueOf(health.getDetails().get("error"));
        assertFalse(error.contains("internal-secret-stacktrace"), "El detalle filtró el cuerpo: " + error);
        assertTrue(error.contains("500"), "El detalle debe incluir el código HTTP: " + error);
    }

    @Test
    @DisplayName("Reporta DOWN cuando MS Vehículos no es alcanzable")
    void shouldReportDownWhenUnreachable() {
        server.expect(once(), requestTo(PROBE_URL)).andRespond(withException(new IOException("Connection refused")));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
    }

    @Test
    @DisplayName("Refleja en los detalles un Circuit Breaker abierto (OPEN)")
    void shouldExposeOpenCircuitBreaker() {
        circuitBreaker.transitionToOpenState();
        server.expect(once(), requestTo(PROBE_URL)).andRespond(withServerError());

        Health health = indicator.health();

        assertEquals("OPEN", health.getDetails().get("circuitBreaker"));
    }

    @Test
    @DisplayName("Reutiliza el resultado en caché dentro del TTL para no saturar MS Vehículos")
    void shouldCacheResultWithinTtl() {
        server.expect(once(), requestTo(PROBE_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        indicator.health();
        clock.advance(TTL.minusSeconds(1));
        Health second = indicator.health();

        assertEquals(Status.UP, second.getStatus());
        server.verify();
    }

    @Test
    @DisplayName("Vuelve a consultar cuando vence el TTL")
    void shouldProbeAgainAfterTtl() {
        server.expect(twice(), requestTo(PROBE_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        indicator.health();
        clock.advance(TTL);
        indicator.health();

        server.verify();
    }

    /** Reloj de prueba que solo avanza cuando la prueba lo indica. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
