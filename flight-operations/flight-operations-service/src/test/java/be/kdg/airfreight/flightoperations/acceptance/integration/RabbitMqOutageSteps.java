package be.kdg.airfreight.flightoperations.acceptance.integration;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story #28: RabbitMQ cannot be reached. Every new connection goes to a port where nothing listens; after the
 * scenario RabbitMQ is reachable again.
 * <p>
 * The class has no Spring annotation on purpose: cucumber-spring registers every step class in the glue packages as
 * a bean itself and refuses one that has {@code @Component}. IntelliJ does not know that and warns about the
 * {@code @Autowired} field, so that warning is suppressed.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class RabbitMqOutageSteps {
    private static final int NOTHING_LISTENS = 1;

    @Autowired
    private CachingConnectionFactory connectionFactory;

    /** Where RabbitMQ really is, to go back to after the scenario. */
    private String address;

    @Given("RabbitMQ is unreachable")
    public void rabbitMqIsUnreachable() {
        address = connectionFactory.getHost() + ":" + connectionFactory.getPort();
        connectionFactory.setAddresses(connectionFactory.getHost() + ":" + NOTHING_LISTENS);
        connectionFactory.resetConnection();
        assertThatThrownBy(connectionFactory::createConnection).isInstanceOf(AmqpConnectException.class);
    }

    @After
    public void rabbitMqIsReachableAgain() {
        if (address != null) {
            connectionFactory.setAddresses(address);
            connectionFactory.resetConnection();
        }
    }
}
