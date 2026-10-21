import {test} from "node:test";
import assert from "node:assert/strict";
import {deckCells, kilograms, offloadingReason, sideWeights} from "./Deck.ts";
import type {DeckCell} from "./Deck.ts";
import type {FlightCargo, LoadPositionOnDeck, Uld} from "./FlightCargo.ts";
import type {LoadCheckReport, LoadCheckResult} from "./LoadCheckReport.ts";

const ROWS = ["C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "P", "R", "S", "T", "U"];

/* The main deck of the 747-400F as the API gives it: nose, fifteen rows of a left and a right position, tail. */
const POSITIONS: LoadPositionOnDeck[] = [
    {name: "A1", side: null}, {name: "A2", side: null}, {name: "B1", side: null},
    ...ROWS.flatMap(row => [{name: `${row}L`, side: "LEFT" as const}, {name: `${row}R`, side: "RIGHT" as const}]),
    {name: "X", side: null},
];

function uld(code: string, destination: string, loadPosition: string | null): Uld {
    return {code, weight: {value: 1000, unit: "KG"}, destination, loadPosition, positionChangedBy: null};
}

function cargo(ulds: Uld[]): FlightCargo {
    return {
        id: "1", flightNumber: "HJ221", scheduledDepartureDate: "2026-01-09", departureAirport: "BRU",
        arrivalAirport: "ATH", aircraftType: "B747-400F",
        std: "2026-01-09T06:30:00Z", sta: "2026-01-09T09:45:00Z",
        etd: "2026-01-09T06:30:00Z", eta: "2026-01-09T09:45:00Z",
        departed: false, finalized: false, finalizedBy: null,
        loadingPeriod: {start: "2026-01-08T18:30:00Z", end: "2026-01-09T06:10:00Z"}, loadCanChange: true,
        ulds, loadPositions: POSITIONS.map(position => position.name),
        floorPlan: {positions: POSITIONS, door: "RL"},
    };
}

function result(changes: Partial<LoadCheckResult>): LoadCheckResult {
    return {checkName: "Offloading", severity: "HARD", passed: true, reason: null, offloading: null,
        lateralBalance: null, ...changes};
}

/* ML (to ATH) cannot leave: PL (to FRA) is in its way. */
const STUCK_ON_ML: LoadCheckReport = {
    hardChecksPassed: false,
    results: [result({
        passed: false,
        reason: "AKE16689R7 on ML cannot be offloaded because AKE20417HJ on PL is in the way",
        offloading: {obstructions: [{
            stuck: {uld: "AKE16689R7", position: "ML"},
            inTheWay: [{uld: "AKE20417HJ", position: "PL"}],
        }]},
    })],
};

function cell(cells: DeckCell[], name: string): DeckCell {
    const found = cells.find(candidate => candidate.name === name);
    assert.ok(found, `a cell ${name}`);
    return found;
}

function marked(cells: DeckCell[]): string[] {
    return cells.filter(candidate => candidate.stuck || candidate.inTheWay)
        .map(candidate => candidate.name + (candidate.stuck ? " stuck" : "") + (candidate.inTheWay ? " in the way" : ""));
}

test("the nose and the tail are on the centre line, each in its own column", () => {
    const cells = deckCells(cargo([]), null, null, false);

    assert.deepEqual(["A1", "A2", "B1", "X"].map(name => [cell(cells, name).column, cell(cells, name).row]),
        [[1, "middle"], [2, "middle"], [3, "middle"], [19, "middle"]]);
});

test("the left and the right position of a row share a column, left at the bottom and right at the top", () => {
    const cells = deckCells(cargo([]), null, null, false);

    assert.deepEqual([cell(cells, "CL").column, cell(cells, "CL").row], [4, "bottom"]);
    assert.deepEqual([cell(cells, "CR").column, cell(cells, "CR").row], [4, "top"]);
    assert.deepEqual([cell(cells, "UL").column, cell(cells, "UR").column], [18, 18]);
});

test("only the position at the cargo door is marked as the door", () => {
    const cells = deckCells(cargo([]), null, null, false);

    assert.deepEqual(cells.filter(candidate => candidate.door).map(candidate => candidate.name), ["RL"]);
});

test("a taken position shows its ULD and whether it goes to the arrival airport of the flight", () => {
    const cells = deckCells(cargo([uld("AKE16689R7", "ATH", "CL"), uld("AKE20417HJ", "FRA", "CR")]), null, null,
        false);

    assert.deepEqual([cell(cells, "CL").uld, cell(cells, "CL").toArrival], ["AKE16689R7", true]);
    assert.deepEqual([cell(cells, "CR").uld, cell(cells, "CR").toArrival], ["AKE20417HJ", false]);
    assert.deepEqual([cell(cells, "DL").uld, cell(cells, "DL").toArrival], [null, null]);
});

test("the ULD that cannot leave and the ULD in its way are marked, nothing else", () => {
    const cells = deckCells(cargo([uld("AKE16689R7", "ATH", "ML"), uld("AKE20417HJ", "FRA", "PL")]), STUCK_ON_ML,
        null, true);

    assert.deepEqual(marked(cells), ["ML stuck", "PL in the way"]);
});

test("without a report nothing is marked", () => {
    const cells = deckCells(cargo([uld("AKE16689R7", "ATH", "ML"), uld("AKE20417HJ", "FRA", "PL")]), null, null,
        true);

    assert.deepEqual(marked(cells), []);
});

test("a report about a ULD that is no longer on that position marks nothing there", () => {
    // the report is of a newer load than the cargo that is drawn: AKE20417HJ has moved from PL to SL since
    const cells = deckCells(cargo([uld("AKE16689R7", "ATH", "ML"), uld("AKE20417HJ", "FRA", "SL")]), STUCK_ON_ML,
        null, true);

    assert.deepEqual(marked(cells), ["ML stuck"]);
});

test("a stuck ULD can also be in the way of the ULD behind it", () => {
    const report: LoadCheckReport = {hardChecksPassed: false, results: [result({
        passed: false,
        reason: "…",
        offloading: {obstructions: [
            {stuck: {uld: "AKE16689R7", position: "ML"},
                inTheWay: [{uld: "AKE20417HJ", position: "RL"}, {uld: "AKE40404R7", position: "PL"}]},
            {stuck: {uld: "AKE40404R7", position: "PL"}, inTheWay: [{uld: "AKE20417HJ", position: "RL"}]},
        ]},
    })]};
    const cells = deckCells(cargo([uld("AKE16689R7", "ATH", "ML"), uld("AKE40404R7", "ATH", "PL"),
        uld("AKE20417HJ", "FRA", "RL")]), report, null, true);

    assert.deepEqual(marked(cells), ["ML stuck", "PL stuck in the way", "RL in the way"]);
});

test("the side weights come from the lateral balance result, with whether it passed", () => {
    const report: LoadCheckReport = {hardChecksPassed: false, results: [result({
        checkName: "Lateral balance", passed: false, reason: "…",
        lateralBalance: {leftKilograms: 1250, rightKilograms: 0, maximumOnTheHeaviestSide: 0, heavierSide: "LEFT"},
    })]};

    assert.deepEqual(sideWeights(report),
        {leftKilograms: 1250, rightKilograms: 0, maximumOnTheHeaviestSide: 0, heavierSide: "LEFT", passed: false});
    assert.equal(sideWeights(null), null);
});

test("the reason of a failed offloading check is shown below the deck", () => {
    assert.equal(offloadingReason(STUCK_ON_ML),
        "AKE16689R7 on ML cannot be offloaded because AKE20417HJ on PL is in the way");
    assert.equal(offloadingReason({hardChecksPassed: true, results: [result({})]}), null);
});

test("a weight is shown in kg rounded to 0.1 kg, as in the sentence of the check", () => {
    assert.equal(kilograms(1200.19841102), "1200.2 kg");
    assert.equal(kilograms(0), "0 kg");
});
