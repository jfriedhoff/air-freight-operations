import {test} from "node:test";
import assert from "node:assert/strict";
import {identityIn, identityQuery} from "./FlightIdentity.ts";

const HJ221 = {flightNumber: "HJ221", scheduledDepartureDate: "2026-01-09", departureAirport: "BRU"};

test("the page of the other context reads the identity it was opened with", () => {
    assert.deepEqual(identityIn(new URLSearchParams(identityQuery(HJ221))), HJ221);
});

test("an address without one of the three parts has no identity", () => {
    assert.equal(identityIn(new URLSearchParams("?flightNumber=HJ221&scheduledDepartureDate=2026-01-09")), null);
    assert.equal(identityIn(new URLSearchParams("?flightNumber=&scheduledDepartureDate=2026-01-09&departureAirport=BRU")),
        null);
});
