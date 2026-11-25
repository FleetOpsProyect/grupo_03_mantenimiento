package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoOpEventPublisherAdapterTest {

    @Test
    @DisplayName("Debe ejecutar publish sin lanzar excepciones")
    void shouldPublishWithoutException() {
        NoOpEventPublisherAdapter adapter = new NoOpEventPublisherAdapter();
        assertDoesNotThrow(() -> adapter.publish("topic-arn", "subject", "payload"));
    }
}
