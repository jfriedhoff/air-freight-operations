/* What identifies a flight in every context: flight number, scheduled departure date (a UTC date) and departure
   airport. A page of the other context is opened with it, never with an internal id. */
export interface FlightIdentity {
    flightNumber: string;
    scheduledDepartureDate: string;
    departureAirport: string;
}

/* The identity in the address of a page, or null when one of its three parts is missing. */
export function identityIn(address: URLSearchParams): FlightIdentity | null {
    const flightNumber = address.get("flightNumber");
    const scheduledDepartureDate = address.get("scheduledDepartureDate");
    const departureAirport = address.get("departureAirport");
    return flightNumber && scheduledDepartureDate && departureAirport
        ? {flightNumber, scheduledDepartureDate, departureAirport}
        : null;
}

/* The query that opens a page by this identity. */
export function identityQuery(identity: FlightIdentity): string {
    return `?${new URLSearchParams({
        flightNumber: identity.flightNumber,
        scheduledDepartureDate: identity.scheduledDepartureDate,
        departureAirport: identity.departureAirport,
    })}`;
}
