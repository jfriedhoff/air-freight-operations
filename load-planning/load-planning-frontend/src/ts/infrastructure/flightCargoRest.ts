import type {FlightCargo} from "../domain/FlightCargo.ts";
import type {FlightsToLoad} from "../domain/FlightToLoad.ts";
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
