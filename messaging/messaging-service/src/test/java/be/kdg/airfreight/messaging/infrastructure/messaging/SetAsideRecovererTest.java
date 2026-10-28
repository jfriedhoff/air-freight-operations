package be.kdg.airfreight.messaging.infrastructure.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetAsideRecovererTest {
    private static final Message MESSAGE = MessageBuilder.withBody("{}".getBytes()).setMessageId("m-42").build();

    private final SetAsideRecoverer recoverer = new SetAsideRecoverer("messaging.dlq");
    private final Logger logger = (Logger) LoggerFactory.getLogger(SetAsideRecoverer.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void stopCapturing() {
        logger.detachAppender(logs);
    }

    @Test
    void aMessageThatFailsForATechnicalReasonIsLoggedAsAnErrorAndSetAside() {
        CannotCreateTransactionException cause = new CannotCreateTransactionException("Connection refused");

        assertThatThrownBy(() -> recoverer.recover(MESSAGE, failure(cause)))
            .isInstanceOf(ListenerExecutionFailedException.class)
            .hasCauseInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).contains("m-42", "messaging.dlq");
            assertThat(event.getThrowableProxy().getCause().getClassName())
                .isEqualTo(CannotCreateTransactionException.class.getName());
        });
    }

    @Test
    void aMessageTheListenerRefusedIsSetAsideWithoutAnotherLine() {
        AmqpRejectAndDontRequeueException refusal = new AmqpRejectAndDontRequeueException("Message m-42 rejected");

        assertThatThrownBy(() -> recoverer.recover(MESSAGE, failure(refusal)))
            .isInstanceOf(ListenerExecutionFailedException.class);
        assertThat(logs.list).isEmpty();
    }

    /** How the listener container hands a failure of the listener to the recoverer. */
    private static ListenerExecutionFailedException failure(Throwable cause) {
        return new ListenerExecutionFailedException("Listener threw exception", cause, MESSAGE);
    }
}
