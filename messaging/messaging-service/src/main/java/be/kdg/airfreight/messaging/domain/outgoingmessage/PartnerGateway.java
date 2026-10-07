package be.kdg.airfreight.messaging.domain.outgoingmessage;

/** The way out to the partners. Throws an unchecked exception when the message could not be handed over. */
public interface PartnerGateway {
    void send(OutgoingMessage message);
}
