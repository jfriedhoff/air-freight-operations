import type {DelayReason, Flight, NewDelay} from "../domain/Flight.ts";
import type {FlightsOfDay} from "../domain/FlightOverview.ts";
import {authorizedFetch} from "./auth.ts";
import {SERVER_URL, throwResponseError} from "./restUtils.ts";

const FLIGHTS_URL = SERVER_URL + "/flights";

/* Without a day the server answers with the flights of today (UTC) and names that day in its answer. */
export async function getFlights(day: string | null): Promise<FlightsOfDay> {
    const parameters = new URLSearchParams();
    if (day) {
        parameters.set("scheduledDepartureDate", day);
    }
    const response = await authorizedFetch(`${FLIGHTS_URL}?${parameters}`);
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De vluchten konden niet opgehaald worden", response);
}

export async function getFlight(flightId: string): Promise<Flight> {
    const response = await authorizedFetch(`${FLIGHTS_URL}/${encodeURIComponent(flightId)}`);
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De vlucht kon niet opgehaald worden", response);
}

export async function getDelayReasons(): Promise<DelayReason[]> {
    const response = await authorizedFetch(SERVER_URL + "/delay-codes");
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De delay reasons konden niet opgehaald worden", response);
}

/* The server answers with the whole flight: a delay moves the ETD and the ETA. */
export async function addDelay(flightId: string, delay: NewDelay): Promise<Flight> {
    const response = await authorizedFetch(`${FLIGHTS_URL}/${encodeURIComponent(flightId)}/delays`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(delay),
    });
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De vertraging is geweigerd", response);
}
