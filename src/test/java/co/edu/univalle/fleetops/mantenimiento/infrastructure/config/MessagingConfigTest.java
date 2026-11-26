package co.edu.univalle.fleetops.mantenimiento.infrastructure.config;

import co.edu.univalle.fleetops.mantenimiento.domain.port.out.EventPublisherPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessagingConfigTest {

    @Test
    @DisplayName("Debe proveer una instancia de EventPublisherPort no-op")
    void shouldProvideNoOpEventPublisher() {
        MessagingConfig config = new MessagingConfig();
        EventPublisherPort publisher = config.noOpEventPublisher();

        assertNotNull(publisher);
    }
}
