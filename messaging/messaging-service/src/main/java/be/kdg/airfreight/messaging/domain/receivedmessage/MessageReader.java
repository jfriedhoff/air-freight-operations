package be.kdg.airfreight.messaging.domain.receivedmessage;

/**
 * Reads one type of incoming message into our own model. One implementation per message type.
 * Throws InvalidMessageException when the message cannot be read or breaks a rule.
 */
public interface MessageReader<T extends IncomingMessage> {
    T read(String body);
}
