package co.edu.univalle.fleetops.mantenimiento.infrastructure.adapter.out.messaging;

import co.edu.univalle.fleetops.mantenimiento.domain.port.out.EventPublisherPort;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter that publishes domain events to AWS SNS.
 */
@Component
@ConditionalOnBean(SnsTemplate.class)
@ConditionalOnProperty(name = "spring.cloud.aws.sns.enabled", havingValue = "true", matchIfMissing = true)
public class SnsEventPublisherAdapter implements EventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(SnsEventPublisherAdapter.class);

    private final SnsTemplate snsTemplate;
    private final String defaultTopicArn;

    public SnsEventPublisherAdapter(
            SnsTemplate snsTemplate,
            @Value("${aws.messaging.sns.topic-arn:}") String defaultTopicArn) {
        this.snsTemplate = snsTemplate;
        this.defaultTopicArn = defaultTopicArn;
    }

    @Override
    public void publish(String destination, String subject, Object payload) {
        String targetTopic = (destination != null && !destination.isBlank()) ? destination : defaultTopicArn;
        if (targetTopic == null || targetTopic.isBlank()) {
            throw new IllegalArgumentException("SNS topic ARN or name must be provided to publish events");
        }
        snsTemplate.sendNotification(targetTopic, payload, subject);
        LOG.debug("Event published to SNS topic={} with subject={}", targetTopic, subject);
    }
}
