import {test} from "node:test";
import assert from "node:assert/strict";
import {throwResponseError} from "./restUtils.ts";

function problem(status: number, detail: string): Response {
    return new Response(JSON.stringify({detail}), {status, headers: {"Content-Type": "application/problem+json"}});
}

test("the error carries the reason the server gives", async () => {
    await assert.rejects(throwResponseError("Het ingeven is geweigerd", problem(409, "The ATD cannot be in the future")),
        {message: "Het ingeven is geweigerd: The ATD cannot be in the future"});
});

test("401 and 403 have no reason from the server, so the page words them", async () => {
    await assert.rejects(throwResponseError("Mislukt", new Response(null, {status: 401})),
        {message: "Mislukt: u bent niet aangemeld"});
    await assert.rejects(throwResponseError("Mislukt", new Response(null, {status: 403})),
        {message: "Mislukt: u hebt geen rechten voor deze actie"});
});

test("an answer that is not Problem Details names the status", async () => {
    await assert.rejects(throwResponseError("Mislukt", new Response("Bad Gateway", {status: 502})),
        {message: "Mislukt: onverwacht antwoord van de server (502)"});
});
