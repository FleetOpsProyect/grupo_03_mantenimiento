package co.edu.univalle.fleetops.mantenimiento.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthStatusMeterBinderTest {

    private static final String METRIC = "app.health.status";

    @Test
    @DisplayName("Registra un gauge por componente de health: 1 si está UP, 0 en otro caso, actualizado en cada lectura")
    void shouldExposeOneGaugePerComponent() {
        Map<String, HealthContributor> contributors = new LinkedHashMap<>();
        contributors.put("db", (HealthIndicator) () -> Health.up().build());
        contributors.put("vehicles", (HealthIndicator) () -> Health.down().build());
        DefaultHealthContributorRegistry contributorRegistry = new DefaultHealthContributorRegistry(contributors);

        AtomicReference<Health> vehiclesHealth = new AtomicReference<>(Health.down().build());
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.healthForPath("db")).thenReturn(Health.up().build());
        when(healthEndpoint.healthForPath("vehicles")).thenAnswer(inv -> vehiclesHealth.get());

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        new HealthStatusMeterBinder(() -> contributorRegistry, () -> healthEndpoint).bindTo(meterRegistry);

        assertEquals(1.0, gauge(meterRegistry, "db"));
        assertEquals(0.0, gauge(meterRegistry, "vehicles"));

        vehiclesHealth.set(Health.up().build());
        assertEquals(1.0, gauge(meterRegistry, "vehicles"));
    }

    @Test
    @DisplayName("Reporta 0 cuando el endpoint no devuelve health para el componente")
    void shouldReportZeroWhenComponentIsMissing() {
        DefaultHealthContributorRegistry contributorRegistry = new DefaultHealthContributorRegistry(
                Map.of("gone", (HealthIndicator) () -> Health.up().build()));
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        new HealthStatusMeterBinder(() -> contributorRegistry, () -> healthEndpoint).bindTo(meterRegistry);

        assertEquals(0.0, gauge(meterRegistry, "gone"));
    }

    @Test
    @DisplayName("Spring Boot lo registra y publica el gauge de un health indicator real")
    void shouldBeWiredBySpringBoot() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MetricsAutoConfiguration.class,
                        SimpleMetricsExportAutoConfiguration.class,
                        HealthContributorAutoConfiguration.class,
                        HealthEndpointAutoConfiguration.class))
                .withBean("customHealthIndicator", HealthIndicator.class, () -> () -> Health.up().build())
                .withBean(HealthStatusMeterBinder.class)
                .run(context -> {
                    MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
                    assertEquals(1.0, gauge(meterRegistry, "custom"));
                });
    }

    private static double gauge(MeterRegistry registry, String component) {
        var gauge = registry.find(METRIC).tag("component", component).gauge();
        assertNotNull(gauge, "Falta el gauge del componente " + component);
        return gauge.value();
    }
}
