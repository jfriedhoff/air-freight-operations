import {holderOf} from "./FlightCargo.ts";
import type {FlightCargo} from "./FlightCargo.ts";
import type {LateralBalance, LoadCheckReport, UldOnDeck} from "./LoadCheckReport.ts";

/* Seen from above with the nose on the left, the left side of the aircraft is the bottom row. */
export type DeckRow = "top" | "middle" | "bottom";

/* One load position on the deck, ready to draw. Everything about the checks comes from the server's report. */
export interface DeckCell {
    name: string;
    column: number;
    row: DeckRow;
    door: boolean;
    uld: string | null;
    // which colour the ULD gets: going to the arrival airport of this flight or not. Only for the eye; whether it
    // can be offloaded is the server's offloading check (stuck, inTheWay)
    toArrival: boolean | null;
    stuck: boolean;
    inTheWay: boolean;
    target: boolean;
    picked: boolean;
}

export type SideWeights = LateralBalance & { passed: boolean };

/* The ULD the load planner picked to place, and where it stood when it was picked (null: in the tray). */
export interface Pick {
    code: string;
    position: string | null;
}

const ROW_OF_SIDE = {LEFT: "bottom", RIGHT: "top"} as const;

/* The columns of the deck from the floor plan in deck order: a position without a side has a column of its own,
   and a left and a right position that follow each other share one. That draws a deck listed nose to tail with
   the left and right position of a row next to each other, as the floor plan of the 747-400F is. */
function columnsOf(cargo: FlightCargo): Map<string, number> {
    const columns = new Map<string, number>();
    let column = 0;
    let open: "LEFT" | "RIGHT" | null = null;
    for (const position of cargo.floorPlan.positions) {
        if (position.side !== null && open !== null && open !== position.side) {
            open = null;
        } else {
            column++;
            open = position.side;
        }
        columns.set(position.name, column);
    }
    return columns;
}

/* A mark of the report only counts where the cargo that is drawn has that same ULD: the cargo and the report are
   two answers, and the report may already be about a newer load. */
function names(marks: UldOnDeck[], cargo: FlightCargo): Set<string> {
    return new Set(marks.filter(mark => holderOf(cargo, mark.position) === mark.uld).map(mark => mark.position));
}

/* The cells of the deck for this cargo, with the marks of the report (none without a report). While a ULD is picked
   and the load can change, its cell is shown as picked and every free position is a place it can go to. */
export function deckCells(cargo: FlightCargo, report: LoadCheckReport | null, picked: string | null,
                          changeable: boolean): DeckCell[] {
    const columns = columnsOf(cargo);
    const obstructions = report?.results.flatMap(result => result.offloading?.obstructions ?? []) ?? [];
    const stuck = names(obstructions.map(obstruction => obstruction.stuck), cargo);
    const inTheWay = names(obstructions.flatMap(obstruction => obstruction.inTheWay), cargo);
    return cargo.floorPlan.positions.map(position => {
        const code = holderOf(cargo, position.name);
        const uld = cargo.ulds.find(candidate => candidate.code === code);
        return {
            name: position.name,
            column: columns.get(position.name)!,
            row: position.side === null ? "middle" : ROW_OF_SIDE[position.side],
            door: position.name === cargo.floorPlan.door,
            uld: code,
            toArrival: uld === undefined ? null : uld.destination === cargo.arrivalAirport,
            stuck: stuck.has(position.name),
            inTheWay: inTheWay.has(position.name),
            target: changeable && picked !== null && code === null,
            picked: changeable && picked !== null && code === picked,
        };
    });
}

/* The weight on each side as the lateral balance check found it, or null without that finding. */
export function sideWeights(report: LoadCheckReport | null): SideWeights | null {
    const result = report?.results.find(candidate => candidate.lateralBalance !== null);
    return result?.lateralBalance ? {...result.lateralBalance, passed: result.passed} : null;
}

/* Why the offloading check failed, as the server says it; null when it passed or there is no report. */
export function offloadingReason(report: LoadCheckReport | null): string | null {
    const result = report?.results.find(candidate => candidate.offloading !== null && !candidate.passed);
    return result?.reason ?? null;
}

/* A weight for the screen, rounded to 0.1 kg as the sentence of the check is: "1200.2 kg". */
export function kilograms(value: number): string {
    return `${Math.round(value * 10) / 10} kg`;
}

/* After a reload the pick stays only while its ULD is still where it was and the load can still change: otherwise
   another planner moved it, or the loading period ended, and the planner must pick again. */
export function keptPick(cargo: FlightCargo, pick: Pick | null, changeable: boolean): Pick | null {
    if (pick === null || !changeable) {
        return null;
    }
    const uld = cargo.ulds.find(candidate => candidate.code === pick.code);
    return uld !== undefined && uld.loadPosition === pick.position ? pick : null;
}
