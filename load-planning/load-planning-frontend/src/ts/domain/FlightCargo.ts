export type WeightUnit = "KG" | "LB";

export interface Weight {
    value: number;
    unit: WeightUnit;
}

export interface Uld {
    code: string;
    weight: Weight;
    destination: string;
    loadPosition: string | null;
    positionChangedBy: string | null;
}

/* A load position as the floor plan lists it, nose to tail. A position in the nose or the tail has no side. */
export interface LoadPositionOnDeck {
    name: string;
    side: "LEFT" | "RIGHT" | null;
}

/* The floor plan of the aircraft: its positions in deck order, and the position at the cargo door. */
export interface FloorPlan {
    positions: LoadPositionOnDeck[];
    door: string;
}

export interface LoadingPeriod {
    start: string;
    end: string;
}

/* The cargo of a flight. All times are UTC instants in ISO-8601, as the API gives them. */
export interface FlightCargo {
    id: string;
    flightNumber: string;
    scheduledDepartureDate: string;
    departureAirport: string;
    arrivalAirport: string;
    aircraftType: string;
    std: string;
    sta: string;
    etd: string;
    eta: string;
    departed: boolean;
    finalized: boolean;
    finalizedBy: string | null;
    loadingPeriod: LoadingPeriod;
    loadCanChange: boolean;
    ulds: Uld[];
    loadPositions: string[];
    floorPlan: FloorPlan;
}

/* Why the load cannot change, as the page words it. OPEN: it can change. */
export type LoadState = "OPEN" | "FINALIZED" | "DEPARTED" | "NOT_STARTED" | "ENDED" | "OUTSIDE_PERIOD";

/* What the page offers. The server decides: it says whether the load can change at this moment, and it refuses
   a change by anyone but a load planner. */
export function canChangeLoad(cargo: FlightCargo, isLoadPlanner: boolean): boolean {
    return isLoadPlanner && cargo.loadCanChange;
}

/* The server says that the load cannot change; the page only chooses the words. Whether the loading period has
   not started or is over is read from the period and the moment the cargo was loaded. If that moment falls
   inside the period after all, the clocks disagree and the page just says "outside the period". */
export function stateOf(cargo: FlightCargo, loadedAt: Date): LoadState {
    if (cargo.finalized) {
        return "FINALIZED";
    }
    if (cargo.departed) {
        return "DEPARTED";
    }
    if (cargo.loadCanChange) {
        return "OPEN";
    }
    if (loadedAt < new Date(cargo.loadingPeriod.start)) {
        return "NOT_STARTED";
    }
    if (loadedAt >= new Date(cargo.loadingPeriod.end)) {
        return "ENDED";
    }
    return "OUTSIDE_PERIOD";
}

/* The booked ULDs that still have no load position: the cargo that would stay behind. */
export function withoutLoadPosition(cargo: FlightCargo): Uld[] {
    return cargo.ulds.filter(uld => uld.loadPosition === null);
}

/* The ULD on this load position, or null when it is free. The page shows it next to the position; whether the
   position may be taken is the server's answer. */
export function holderOf(cargo: FlightCargo, position: string): string | null {
    return cargo.ulds.find(uld => uld.loadPosition === position)?.code ?? null;
}
