package be.kdg.airfreight.messaging.domain.receivedmessage;

import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;

/** What every message offers once it has been read: the id a duplicate is recognised by, and its version. */
public interface IncomingMessage {
    MessageId id();

    FormatVersion version();
}
