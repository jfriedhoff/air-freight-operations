import {test} from "node:test";
import assert from "node:assert/strict";
import {listQuery, showsEta, showsEtd} from "./FlightToLoad.ts";

test("the list hides departed flights unless the address asks for them", () => {
    assert.equal(listQuery("2026-01-09", false), "?scheduledDepartureDate=2026-01-09");
    assert.equal(listQuery("2026-01-09", true), "?scheduledDepartureDate=2026-01-09&includeDeparted=true");
});

test("an estimated time is only shown when it differs from the scheduled time", () => {
    assert.equal(showsEtd({std: "2026-01-09T06:30:00Z", etd: "2026-01-09T06:30:00Z"}), false);
    assert.equal(showsEtd({std: "2026-01-09T06:30:00Z", etd: "2026-01-09T06:45:00Z"}), true);
    assert.equal(showsEta({sta: "2026-01-09T08:00:00Z", eta: "2026-01-09T08:15:00Z"}), true);
});
