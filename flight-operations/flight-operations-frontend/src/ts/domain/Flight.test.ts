import {test} from "node:test";
import assert from "node:assert/strict";
import {canRegisterAta, canRegisterAtd, delayKindsToAdd, showsEta, showsEtd} from "./Flight.ts";
import type {Flight, FlightStatus} from "./Flight.ts";

function flight(status: FlightStatus, changes: Partial<Flight> = {}): Flight {
    return {
        id: "1", flightNumber: "HJ221", scheduledDepartureDate: "2026-01-09",
        departureAirport: {code: "BRU", name: null}, arrivalAirport: {code: "LEJ", name: null},
        registration: "OO-ABC", status,
        std: "2026-01-09T06:30:00Z", sta: "2026-01-09T08:00:00Z",
        etd: "2026-01-09T06:30:00Z", eta: "2026-01-09T08:00:00Z",
        atd: null, atdRegisteredBy: null, ata: null, ataRegisteredBy: null, delays: [],
        ...changes,
    };
}

test("a flight planner can add both kinds of delay to a planned flight", () => {
    assert.deepEqual(delayKindsToAdd(flight("PLANNED"), true), ["DEPARTURE", "ARRIVAL"]);
});

test("after departure only an arrival delay is offered", () => {
    assert.deepEqual(delayKindsToAdd(flight("DEPARTED"), true), ["ARRIVAL"]);
});

test("a landed flight gets no delay form", () => {
    assert.deepEqual(delayKindsToAdd(flight("LANDED"), true), []);
});

test("a load planner gets no delay form", () => {
    assert.deepEqual(delayKindsToAdd(flight("PLANNED"), false), []);
});

test("a flight planner can register the ATD of a planned flight", () => {
    assert.equal(canRegisterAtd(flight("PLANNED"), true), true);
});

test("a departed flight gets no ATD form", () => {
    assert.equal(canRegisterAtd(flight("DEPARTED"), true), false);
    assert.equal(canRegisterAtd(flight("LANDED"), true), false);
});

test("a load planner gets no ATD form", () => {
    assert.equal(canRegisterAtd(flight("PLANNED"), false), false);
});

test("a flight planner can register the ATA of a departed flight", () => {
    assert.equal(canRegisterAta(flight("DEPARTED"), true), true);
});

test("a planned or landed flight gets no ATA form", () => {
    assert.equal(canRegisterAta(flight("PLANNED"), true), false);
    assert.equal(canRegisterAta(flight("LANDED"), true), false);
});

test("a load planner gets no ATA form", () => {
    assert.equal(canRegisterAta(flight("DEPARTED"), false), false);
});

test("an estimated time is only shown when it differs from the scheduled time", () => {
    assert.equal(showsEtd(flight("PLANNED")), false);
    assert.equal(showsEta(flight("PLANNED")), false);
    const delayed = flight("PLANNED", {etd: "2026-01-09T06:45:00Z", eta: "2026-01-09T08:15:00Z"});
    assert.equal(showsEtd(delayed), true);
    assert.equal(showsEta(delayed), true);
});
