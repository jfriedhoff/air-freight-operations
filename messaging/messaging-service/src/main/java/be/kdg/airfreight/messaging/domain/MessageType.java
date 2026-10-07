package be.kdg.airfreight.messaging.domain;

public enum MessageType {
    /** Incoming. */
    SSM,
    ULD,
    FLIGHT_EVENT,
    LOAD_FINALIZED,
    /** To the partners. */
    FLM,
    MVT_ED,
    MVT_EA,
    MVT_AD,
    MVT_AA,
    CPM
}
