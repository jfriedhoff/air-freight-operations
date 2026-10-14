package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

import be.kdg.airfreight.messaging.application.ScheduleService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Inbound adapter: a controller, but for messages. Hands the body over untouched. */
@Component
class SsmListener {
    private final ScheduleService schedule;

    SsmListener(ScheduleService schedule) {
        this.schedule = schedule;
    }

    @RabbitListener(queues = "${messaging.amqp.schedule-queue}")
    void on(Message message) {
        schedule.receiveSsm(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
