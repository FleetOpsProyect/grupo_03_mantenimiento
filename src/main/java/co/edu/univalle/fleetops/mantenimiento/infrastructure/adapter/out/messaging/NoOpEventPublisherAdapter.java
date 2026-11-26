package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.messaging;

import co.edu.univalle.fleetops.mantenimiento.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación no-op de EventPublisherPort cuando AWS SNS está deshabilitado (p. ej. en pruebas).
 */
public class NoOpEventPublisherAdapter implements EventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpEventPublisherAdapter.class);

    @Override
    public void publish(String destination, String subject, Object payload) {
        LOG.debug("No-op publish (SNS deshabilitado): destination={}, subject={}", destination, subject);
    }
}
