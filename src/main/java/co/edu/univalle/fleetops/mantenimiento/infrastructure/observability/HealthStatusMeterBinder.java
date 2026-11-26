package co.edu.univalle.fleetops.mantenimiento.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Publica cada componente de health como un gauge de Prometheus: {@code app_health_status{component="db"}}.
 * Vale 1 cuando el componente está UP y 0 en cualquier otro estado.
 *
 * <p>Los health indicators siguen siendo la única fuente de verdad: los mismos chequeos alimentan el JSON
 * de {@code /actuator/health} y los dashboards de Grafana. Las dependencias se resuelven de forma diferida
 * para no crear el endpoint de health mientras el registro de métricas todavía se está construyendo.
 */
@Component
public class HealthStatusMeterBinder implements MeterBinder {

    static final String METRIC_NAME = "app.health.status";

    private final Supplier<HealthContributorRegistry> contributorRegistry;
    private final Supplier<HealthEndpoint> healthEndpoint;

    @Autowired
    public HealthStatusMeterBinder(ObjectProvider<HealthContributorRegistry> contributorRegistry,
                                   ObjectProvider<HealthEndpoint> healthEndpoint) {
        this(contributorRegistry::getObject, healthEndpoint::getObject);
    }

    HealthStatusMeterBinder(Supplier<HealthContributorRegistry> contributorRegistry,
                            Supplier<HealthEndpoint> healthEndpoint) {
        this.contributorRegistry = contributorRegistry;
        this.healthEndpoint = healthEndpoint;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (NamedContributor<HealthContributor> contributor : contributorRegistry.get()) {
            String component = contributor.getName();
            Gauge.builder(METRIC_NAME, this, binder -> binder.statusValue(component))
                    .tag("component", component)
                    .description("Estado de salud del componente: 1 = UP, 0 = cualquier otro estado")
                    .register(registry);
        }
    }

    private double statusValue(String component) {
        HealthComponent health = healthEndpoint.get().healthForPath(component);
        return health != null && Status.UP.equals(health.getStatus()) ? 1.0 : 0.0;
    }
}
