package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.messaging;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SnsEventPublisherAdapterTest {

    @Mock
    private SnsTemplate snsTemplate;

    @Test
    void publishUsesExplicitDestinationWhenProvided() {
        SnsEventPublisherAdapter adapter = new SnsEventPublisherAdapter(snsTemplate, "arn:aws:sns:us-east-1:000:default");
        Object payload = "{\"event\":\"test\"}";

        adapter.publish("arn:aws:sns:us-east-1:000:custom", "mantenimiento.finalizado", payload);

        verify(snsTemplate).sendNotification("arn:aws:sns:us-east-1:000:custom", payload, "mantenimiento.finalizado");
    }

    @Test
    void publishFallsBackToDefaultTopicArnWhenDestinationIsBlank() {
        SnsEventPublisherAdapter adapter = new SnsEventPublisherAdapter(snsTemplate, "arn:aws:sns:us-east-1:000:default");
        Object payload = "{\"event\":\"test\"}";

        adapter.publish("  ", "mantenimiento.finalizado", payload);

        verify(snsTemplate).sendNotification("arn:aws:sns:us-east-1:000:default", payload, "mantenimiento.finalizado");
    }

    @Test
    void publishThrowsWhenDestinationAndDefaultAreMissing() {
        SnsEventPublisherAdapter adapter = new SnsEventPublisherAdapter(snsTemplate, "");

        assertThrows(IllegalArgumentException.class,
                () -> adapter.publish(null, "mantenimiento.finalizado", "{}"));
    }
}
