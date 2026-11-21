package be.kdg.airfreight.simulator;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/** Reads back what the simulator sent. A bean of the test source set only. */
@Component
public class StreamProbe {
    private static final long WAIT_FOR_A_MESSAGE_MS = 2000;
    private static final long WAIT_FOR_SILENCE_MS = 300;

    private final RabbitTemplate rabbit;
    private final AmqpAdmin admin;

    StreamProbe(RabbitTemplate rabbit, AmqpAdmin admin) {
        this.rabbit = rabbit;
        this.admin = admin;
    }

    /** Before every test: what an earlier test left behind must not be read by this one. */
    public void clear() {
        admin.purgeQueue(StreamProbeConfig.SCHEDULE_QUEUE);
        admin.purgeQueue(StreamProbeConfig.BOOKINGS_QUEUE);
    }

    public SentMessage nextSsm() {
        return next(StreamProbeConfig.SCHEDULE_QUEUE);
    }

    public SentMessage nextUldMessage() {
        return next(StreamProbeConfig.BOOKINGS_QUEUE);
    }

    /** The simulator publishes before it answers, so a short wait is enough to see that nothing came. */
    public boolean nothingSent() {
        return rabbit.receive(StreamProbeConfig.SCHEDULE_QUEUE, WAIT_FOR_SILENCE_MS) == null
            && rabbit.receive(StreamProbeConfig.BOOKINGS_QUEUE, WAIT_FOR_SILENCE_MS) == null;
    }

    private SentMessage next(String queue) {
        Message message = rabbit.receive(queue, WAIT_FOR_A_MESSAGE_MS);
        assertThat(message).as("a message on %s", queue).isNotNull();
        return new SentMessage(message.getMessageProperties().getReceivedRoutingKey(),
            message.getMessageProperties().getContentType(), message.getMessageProperties().getMessageId(),
            new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
