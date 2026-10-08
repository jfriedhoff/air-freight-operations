package be.kdg.airfreight.simulator.infrastructure.messaging;

/** Sends a message on the stream it belongs to. The application only knows this, not the broker. */
public interface StreamPublisher {
    void send(SsmMessage ssm);

    void send(UldMessage uldMessage);
}
