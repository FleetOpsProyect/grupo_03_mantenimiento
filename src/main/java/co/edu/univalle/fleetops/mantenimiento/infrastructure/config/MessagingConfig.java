package co.edu.univalle.fleetops.mantenimiento.infrastructure.config;

import co.edu.univalle.fleetops.mantenimiento.domain.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.messaging.NoOpEventPublisherAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería para proveer implementación no-op cuando SNS esté deshabilitado.
 */
@Configuration
public class MessagingConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingConfig.class);

    @Bean
    @ConditionalOnMissingBean(EventPublisherPort.class)
    public EventPublisherPort noOpEventPublisher() {
        LOG.info("AWS SNS deshabilitado o sin bean SnsTemplate. Registrando NoOpEventPublisherAdapter.");
        return new NoOpEventPublisherAdapter();
    }
}
