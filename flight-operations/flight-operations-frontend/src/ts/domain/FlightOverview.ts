/* One row of the overview: the identity of a flight and its scheduled times. Times are UTC instants in
   ISO-8601, as the API gives them; the scheduled departure date is a UTC date. */
export interface FlightRow {
    id: string;
    flightNumber: string;
    scheduledDepartureDate: string;
    departureAirport: string;
    arrivalAirport: string;
    std: string;
    sta: string;
}

/* The flights of one day; the server names the day, also when the page did not ask for one. */
export interface FlightsOfDay {
    scheduledDepartureDate: string;
    flights: FlightRow[];
}

/* The query of the overview for a day: the address shows the choice, so reload, back and the link from
   Vluchtdetails give the same list. */
export function overviewQuery(day: string): string {
    return `?${new URLSearchParams({scheduledDepartureDate: day})}`;
}
