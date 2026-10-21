import {test} from "node:test";
import assert from "node:assert/strict";
import {canChangeLoad, holderOf, stateOf, withoutLoadPosition} from "./FlightCargo.ts";
import type {FlightCargo, Uld} from "./FlightCargo.ts";

const START = "2026-01-08T18:30:00Z";
const END = "2026-01-09T06:10:00Z";

function uld(code: string, loadPosition: string | null): Uld {
    return {code, weight: {value: 1000, unit: "KG"}, destination: "LEJ", loadPosition, positionChangedBy: null};
}

function cargo(changes: Partial<FlightCargo> = {}): FlightCargo {
    return {
        id: "1", flightNumber: "HJ221", scheduledDepartureDate: "2026-01-09", departureAirport: "BRU",
        arrivalAirport: "LEJ", aircraftType: "B744",
        std: "2026-01-09T06:30:00Z", sta: "2026-01-09T08:00:00Z",
        etd: "2026-01-09T06:30:00Z", eta: "2026-01-09T08:00:00Z",
        departed: false, finalized: false, finalizedBy: null,
        loadingPeriod: {start: START, end: END}, loadCanChange: false,
        ulds: [], loadPositions: [],
        ...changes,
    };
}

test("only a load planner may change a load that the server says can change", () => {
    assert.equal(canChangeLoad(cargo({loadCanChange: true}), true), true);
    assert.equal(canChangeLoad(cargo({loadCanChange: true}), false), false);
    assert.equal(canChangeLoad(cargo({loadCanChange: false}), true), false);
});

test("a finalized load is named finalized, also after departure", () => {
    assert.equal(stateOf(cargo({finalized: true, departed: true}), new Date(END)), "FINALIZED");
});

test("a departed flight is named departed", () => {
    assert.equal(stateOf(cargo({departed: true}), new Date(END)), "DEPARTED");
});

test("a load that can change is open", () => {
    assert.equal(stateOf(cargo({loadCanChange: true}), new Date("2026-01-09T00:00:00Z")), "OPEN");
});

test("before the loading period it has not started", () => {
    assert.equal(stateOf(cargo(), new Date("2026-01-08T18:29:00Z")), "NOT_STARTED");
});

test("from the end of the loading period it has ended", () => {
    assert.equal(stateOf(cargo(), new Date(END)), "ENDED");
});

test("inside the period while the server says no, the clocks disagree and the page says outside the period", () => {
    assert.equal(stateOf(cargo(), new Date(START)), "OUTSIDE_PERIOD");
});

test("the ULDs without a load position are the cargo that would stay behind", () => {
    const flight = cargo({ulds: [uld("AKE12345HJ", "11L"), uld("AKE67890HJ", null)]});
    assert.deepEqual(withoutLoadPosition(flight).map(u => u.code), ["AKE67890HJ"]);
});

test("a load position is held by the ULD on it, and by nobody when it is free", () => {
    const withTwo = cargo({ulds: [uld("AKE16689R7", "CL"), uld("AKE20417HJ", null)]});

    assert.equal(holderOf(withTwo, "CL"), "AKE16689R7");
    assert.equal(holderOf(withTwo, "CR"), null);
});
