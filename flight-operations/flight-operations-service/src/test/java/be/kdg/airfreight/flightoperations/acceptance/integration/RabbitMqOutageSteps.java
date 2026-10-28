package be.kdg.airfreight.flightoperations.acceptance.integration;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story #28: RabbitMQ cannot be reached. Every new connection goes to a port where nothing listens; after the
 * scenario RabbitMQ is reachable again.
 */
public class RabbitMqOutageSteps {
    private static final int NOTHING_LISTENS = 1;

    private final CachingConnectionFactory connectionFactory;

    /** Where RabbitMQ really is, to go back to after the scenario. */
    private String address;

    public RabbitMqOutageSteps(CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Given("RabbitMQ is unreachable")
    public void rabbitMqIsUnreachable() {
        address = connectionFactory.getHost() + ":" + connectionFactory.getPort();
        connectionFactory.setAddresses(connectionFactory.getHost() + ":" + NOTHING_LISTENS);
        connectionFactory.resetConnection();
        assertThatThrownBy(connectionFactory::createConnection).isInstanceOf(AmqpConnectException.class);
    }

    /** Whether this scenario made RabbitMQ unreachable. */
    boolean isActive() {
        return address != null;
    }

    @After
    public void rabbitMqIsReachableAgain() {
        if (address != null) {
            connectionFactory.setAddresses(address);
            connectionFactory.resetConnection();
        }
    }
}
