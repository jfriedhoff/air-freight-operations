package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

import be.kdg.airfreight.messaging.application.LoadService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class LoadFinalizedListener {
    private final LoadService loads;

    LoadFinalizedListener(LoadService loads) {
        this.loads = loads;
    }

    @RabbitListener(queues = MessagingConfig.LOADS_QUEUE)
    void on(Message message) {
        loads.receiveLoadFinalized(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
