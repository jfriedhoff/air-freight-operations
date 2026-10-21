package be.kdg.airfreight.loadplanning.domain.flightcargo;

/**
 * What a load check found, as data next to its sentence, so the cargo page can show it on the deck. A check
 * without anything to show has no details. A new kind of details is added here and mapped in the API; a check
 * without details touches nothing.
 */
public sealed interface LoadCheckDetails permits OffloadingDetails {
}
