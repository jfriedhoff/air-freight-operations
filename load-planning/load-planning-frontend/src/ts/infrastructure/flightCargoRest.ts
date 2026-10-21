import type {FlightCargo} from "../domain/FlightCargo.ts";
import type {FlightIdentity} from "../domain/FlightIdentity.ts";
import type {FlightsToLoad, FlightToLoad} from "../domain/FlightToLoad.ts";
import type {FinalizeOutcome} from "../domain/LoadCheckReport.ts";
import {authorizedFetch} from "./auth.ts";
import {SERVER_URL, throwResponseError} from "./restUtils.ts";

const FLIGHT_CARGOS_URL = SERVER_URL + "/flight-cargos";

/* Without a day the server answers with the flights of today and names that day in its answer. */
export async function getFlightsToLoad(day: string | null, includeDeparted: boolean): Promise<FlightsToLoad> {
    const parameters = new URLSearchParams();
    if (day) {
        parameters.set("scheduledDepartureDate", day);
    }
    if (includeDeparted) {
        parameters.set("includeDeparted", "true");
    }
    const response = await authorizedFetch(`${FLIGHT_CARGOS_URL}?${parameters}`);
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De te beladen vluchten konden niet opgehaald worden", response);
}

/* The cargo of the flight with this identity, departed or not; null when Load Planning does not know the flight
   (yet). The server matches the identity. */
export async function findFlightCargo(identity: FlightIdentity): Promise<FlightToLoad | null> {
    const parameters = new URLSearchParams({
        scheduledDepartureDate: identity.scheduledDepartureDate,
        flightNumber: identity.flightNumber,
        departureAirport: identity.departureAirport,
        includeDeparted: "true",
    });
    const response = await authorizedFetch(`${FLIGHT_CARGOS_URL}?${parameters}`);
    if (response.ok) {
        const flightsToLoad: FlightsToLoad = await response.json();
        return flightsToLoad.flights[0] ?? null;
    }
    return throwResponseError("De lading van de vlucht kon niet gevonden worden", response);
}

export async function getFlightCargo(flightCargoId: string): Promise<FlightCargo> {
    const response = await authorizedFetch(`${FLIGHT_CARGOS_URL}/${encodeURIComponent(flightCargoId)}`);
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De lading van de vlucht kon niet opgehaald worden", response);
}

/* The server answers with the whole cargo: the page redraws from that one answer. */
export async function assignLoadPosition(flightCargoId: string, uldCode: string,
                                         loadPosition: string): Promise<FlightCargo> {
    const url = `${FLIGHT_CARGOS_URL}/${encodeURIComponent(flightCargoId)}/ulds/${encodeURIComponent(uldCode)}`
        + "/load-position";
    const response = await authorizedFetch(url, {
        method: "PUT",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({loadPosition}),
    });
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De laadpositie is geweigerd", response);
}

/* Takes the ULD off its load position; the server answers with the whole cargo, like an assignment. */
export async function clearLoadPosition(flightCargoId: string, uldCode: string): Promise<FlightCargo> {
    const url = `${FLIGHT_CARGOS_URL}/${encodeURIComponent(flightCargoId)}/ulds/${encodeURIComponent(uldCode)}`
        + "/load-position";
    const response = await authorizedFetch(url, {method: "DELETE"});
    if (response.ok) {
        return response.json();
    }
    return throwResponseError("De laadpositie kon niet leeggemaakt worden", response);
}

/* The server answers with the report both ways: 200 when the load is finalized, 409 with the code
   hard-check-failed and the report when a hard check fails. Any other refusal, such as outside the loading
   period, is an error with its reason. */
export async function finalizeLoad(flightCargoId: string): Promise<FinalizeOutcome> {
    const response = await authorizedFetch(`${FLIGHT_CARGOS_URL}/${encodeURIComponent(flightCargoId)}/finalize`, {
        method: "POST",
    });
    if (response.ok) {
        return {finalized: true, report: await response.json()};
    }
    // a copy, because throwResponseError reads the body again
    const problem = await response.clone().json().catch(() => null);
    if (problem?.code === "hard-check-failed") {
        return {finalized: false, report: problem.report};
    }
    return throwResponseError("De lading is niet gefinaliseerd", response);
}
