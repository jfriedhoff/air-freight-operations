package be.kdg.airfreight.simulator;

/** What a test reads back from RabbitMQ: the properties partners can see, and the JSON body. */
public record SentMessage(String routingKey, String contentType, String messageId, String body) {
}
