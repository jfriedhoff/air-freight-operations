package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Sends one message and waits for RabbitMQ's confirm of that message, for the event relay and the partner
 * gateway. A message RabbitMQ did not take must stay in its outbox, not count as sent. The confirm is
 * correlated: when RabbitMQ refuses the message by closing the channel (an exchange that does not exist),
 * Spring answers the waiting future with a nack. A simple waitForConfirmsOrDie could miss that close, because
 * Spring replaces a closed channel with a new one, and on the new channel there is nothing left to wait for.
 * The wait happens inside invoke, so this thread keeps the channel until the confirm is in: otherwise Spring
 * would put the channel back in its cache with the confirm still open and wait for it on a background thread,
 * which fails there once RabbitMQ closed the channel.
 */
final class ConfirmedSend {
    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    private ConfirmedSend() {
    }

    static void send(RabbitTemplate rabbit, String exchange, String routingKey, Message message) {
        CorrelationData confirmation = new CorrelationData(message.getMessageProperties().getMessageId());
        Confirm confirm = rabbit.invoke(operations -> {
            operations.send(exchange, routingKey, message, confirmation);
            return awaitConfirm(confirmation);
        });
        if (!confirm.ack()) {
            throw new AmqpException("RabbitMQ did not take the message: " + confirm.reason());
        }
    }

    private static Confirm awaitConfirm(CorrelationData confirmation) {
        try {
            return confirmation.getFuture().get(CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while waiting for the confirm of RabbitMQ", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AmqpException("RabbitMQ did not confirm the message in time", e);
        }
    }
}
