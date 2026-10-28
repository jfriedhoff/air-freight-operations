package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

/**
 * What happens to a message after its last retry: it is rejected, so RabbitMQ sets it aside on the dead letter
 * queue, as before. A message the listener refused itself was already logged as a warning, with its reason. Any
 * other failure is a fault of ours, such as a database that cannot be reached: that is logged as an error, with
 * the cause, so someone can repair it and send the message again.
 * Spring's own RejectAndDontRequeueRecoverer does the same rejection, but logs every message as a warning.
 */
@Slf4j
class SetAsideRecoverer implements MessageRecoverer {
    private final String deadLetterQueue;

    SetAsideRecoverer(String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        if (!refusedByTheListener(cause)) {
            log.error("Message {} could not be processed after its retries and is set aside on {}",
                message.getMessageProperties().getMessageId(), deadLetterQueue, cause);
        }
        // the container rejects a message without requeue when this exception is among the causes
        throw new ListenerExecutionFailedException("Set aside after its retries",
            new AmqpRejectAndDontRequeueException(cause), message);
    }

    private static boolean refusedByTheListener(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof AmqpRejectAndDontRequeueException) {
                return true;
            }
        }
        return false;
    }
}
