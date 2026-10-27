export type DelayKind = "DEPARTURE" | "ARRIVAL";
export type FlightStatus = "PLANNED" | "DEPARTED" | "LANDED";

export interface Airport {
    code: string;
    name: string | null;
}

export interface DelayReason {
    code: string;
    description: string | null;
}

export interface Delay {
    id: string;
    kind: DelayKind;
    minutes: number;
    reason: DelayReason;
    description: string;
    lastChangedBy: string | null;
}

/* All times are UTC instants in ISO-8601, as the API gives them. */
export interface Flight {
    id: string;
    flightNumber: string;
    scheduledDepartureDate: string;
    departureAirport: Airport;
    arrivalAirport: Airport;
    registration: string;
    status: FlightStatus;
    std: string;
    sta: string;
    etd: string;
    eta: string;
    atd: string | null;
    atdRegisteredBy: string | null;
    ata: string | null;
    ataRegisteredBy: string | null;
    delays: Delay[];
}

export interface NewDelay {
    kind: DelayKind;
    minutes: number;
    reason: string;
    description: string;
}

/* An estimated time is only shown when it differs from the scheduled time. */
export function showsEtd(flight: Flight): boolean {
    return flight.etd !== flight.std;
}

export function showsEta(flight: Flight): boolean {
    return flight.eta !== flight.sta;
}

/* Which delays the page offers. The server decides: it refuses a departure delay on a flight that is not planned,
   an arrival delay on a flight that has landed, and a change by anyone but a flight planner. */
export function delayKindsToAdd(flight: Flight, isFlightPlanner: boolean): DelayKind[] {
    if (!isFlightPlanner || flight.status === "LANDED") {
        return [];
    }
    return flight.status === "PLANNED" ? ["DEPARTURE", "ARRIVAL"] : ["ARRIVAL"];
}

/* The page offers the ATD to a flight planner while the flight is planned. Whether the CPM has been processed
   is only known to the server: it refuses the ATD with the reason. */
export function canRegisterAtd(flight: Flight, isFlightPlanner: boolean): boolean {
    return isFlightPlanner && flight.status === "PLANNED";
}
