package be.kdg.airfreight.loadplanning.acceptance.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/** Keeps what Load Planning logs during one scenario, for the steps that check "... is logged". */
public class LogCapture {
    private final Logger logger = (Logger) LoggerFactory.getLogger("be.kdg.airfreight.loadplanning");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @Before
    public void start() {
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void stop() {
        logger.detachAppender(appender);
    }

    void assertLogged(Level level, String... fragments) {
        assertThat(appender.list)
            .as("%s log lines containing %s", level, String.join(", ", fragments))
            .anyMatch(event -> event.getLevel() == level && containsAll(event.getFormattedMessage(), fragments));
    }

    private static boolean containsAll(String message, String... fragments) {
        for (String fragment : fragments) {
            if (!message.contains(fragment)) {
                return false;
            }
        }
        return true;
    }
}
