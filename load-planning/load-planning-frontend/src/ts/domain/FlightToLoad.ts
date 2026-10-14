/* One row of the list of flights to load. All times are UTC instants in ISO-8601, as the API gives them. */
export interface FlightToLoad {
    id: string;
    flightNumber: string;
    scheduledDepartureDate: string;
    departureAirport: string;
    arrivalAirport: string;
    std: string;
    sta: string;
    etd: string;
    eta: string;
    departed: boolean;
    finalized: boolean;
}

/* The flights of one day; the server names the day, also when the page did not ask for one. */
export interface FlightsToLoad {
    scheduledDepartureDate: string;
    flights: FlightToLoad[];
}

/* An estimated time is only shown when it differs from the scheduled time. */
export function showsEtd(flight: { std: string, etd: string }): boolean {
    return flight.etd !== flight.std;
}

export function showsEta(flight: { sta: string, eta: string }): boolean {
    return flight.eta !== flight.sta;
}

/* The query of the list page for a day: the address shows the choice, so reload, back and the link from the
   cargo page give the same list. Departed flights are hidden unless the address says otherwise. */
export function listQuery(day: string, includeDeparted: boolean): string {
    const parameters = new URLSearchParams({scheduledDepartureDate: day});
    if (includeDeparted) {
        parameters.set("includeDeparted", "true");
    }
    return `?${parameters}`;
}
