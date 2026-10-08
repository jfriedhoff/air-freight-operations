package be.kdg.airfreight.simulator.api;

import java.util.List;
import java.util.UUID;

/** The id of every message that was sent, in the order of the supplied records. */
public record SentMessagesDto(List<UUID> messageIds) {
}
