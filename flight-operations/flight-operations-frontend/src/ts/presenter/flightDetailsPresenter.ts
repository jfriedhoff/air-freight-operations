import {delayKindsToAdd, showsEta, showsEtd} from "../domain/Flight.ts";
import type {Airport, Delay, DelayKind, DelayReason, Flight} from "../domain/Flight.ts";
import {overviewQuery} from "../domain/FlightOverview.ts";
import {FLIGHT_PLANNER} from "../infrastructure/auth.ts";
import {addDelay, getDelayReasons, getFlight} from "../infrastructure/flightRest.ts";
import {brusselsDateTime, plainDate} from "./brusselsTime.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {addError, addInfo, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const backToOverview = document.getElementById("back-to-overview") as HTMLAnchorElement;
const details = document.getElementById("flight-details")!;
const delayRows = document.getElementById("delays")!;
const noDelays = document.getElementById("no-delays")!;
const delayForm = document.getElementById("delay-form") as HTMLFormElement;
const kindField = document.getElementById("kind") as HTMLSelectElement;
const minutesField = document.getElementById("minutes") as HTMLInputElement;
const reasonField = document.getElementById("reason") as HTMLSelectElement;
const descriptionField = document.getElementById("description") as HTMLTextAreaElement;

const DELAY_KIND = {DEPARTURE: "Vertrek", ARRIVAL: "Aankomst"};
const DELAY_ADDED = {DEPARTURE: "De vertrekvertraging is geregistreerd.", ARRIVAL: "De aankomstvertraging is geregistreerd."};
const STATUS = {PLANNED: "Gepland", DEPARTED: "Opgestegen", LANDED: "Geland"};

const flightId = new URLSearchParams(window.location.search).get("flightId");
let isFlightPlanner = false;

function text(id: string, value: string): void {
    document.getElementById(id)!.textContent = value;
}

function airport(airport: Airport): string {
    return airport.name ? `${airport.code} – ${airport.name}` : airport.code;
}

/* Shows the row only when there is something to show: an estimated time that differs, a registered actual time. */
function optionalTime(rowId: string, valueId: string, instant: string | null, shown: boolean): void {
    document.getElementById(rowId)!.classList.toggle("hidden", !shown || instant === null);
    text(valueId, instant === null ? "" : brusselsDateTime(instant));
}

function delayRow(delay: Delay): HTMLTableRowElement {
    const row = document.createElement("tr");
    const reason = delay.reason.description ? `${delay.reason.code} – ${delay.reason.description}` : delay.reason.code;
    for (const cell of [DELAY_KIND[delay.kind], `${delay.minutes} min`, reason, delay.description]) {
        row.appendChild(createElementWithText("td", cell));
    }
    return row;
}

function kindOption(kind: DelayKind): HTMLElement {
    const option = createElementWithText("option", DELAY_KIND[kind]);
    option.setAttribute("value", kind);
    return option;
}

function renderFlight(flight: Flight): void {
    backToOverview.href = "../index.html" + overviewQuery(flight.scheduledDepartureDate);
    backToOverview.textContent = `← Overzicht vluchten van ${plainDate(flight.scheduledDepartureDate)}`;
    text("flight-number", flight.flightNumber);
    text("scheduled-departure-date", plainDate(flight.scheduledDepartureDate));
    text("status", STATUS[flight.status]);
    text("departure-airport", airport(flight.departureAirport));
    text("arrival-airport", airport(flight.arrivalAirport));
    text("std", brusselsDateTime(flight.std));
    text("sta", brusselsDateTime(flight.sta));
    optionalTime("etd-row", "etd", flight.etd, showsEtd(flight));
    optionalTime("eta-row", "eta", flight.eta, showsEta(flight));
    optionalTime("atd-row", "atd", flight.atd, true);
    optionalTime("ata-row", "ata", flight.ata, true);

    delayRows.replaceChildren(...flight.delays.map(delayRow));
    noDelays.classList.toggle("hidden", flight.delays.length > 0);
    const kinds = delayKindsToAdd(flight, isFlightPlanner);
    kindField.replaceChildren(...kinds.map(kindOption));
    delayForm.classList.toggle("hidden", kinds.length === 0);
    // only with a flight on the screen: a load planner reads why there is no form
    document.getElementById("role-note")!.classList.toggle("hidden", isFlightPlanner);
    details.classList.remove("hidden");
}

function renderDelayReasons(reasons: DelayReason[]): void {
    for (const reason of reasons) {
        const option = createElementWithText("option", `${reason.code} – ${reason.description ?? ""}`);
        option.setAttribute("value", reason.code);
        reasonField.appendChild(option);
    }
}

/* The form only checks that something was entered; whether the delay is allowed is the server's answer. */
async function enterDelay(event: Event): Promise<void> {
    event.preventDefault();
    clearMessages(messageBox);
    if (!flightId || !delayForm.reportValidity()) {
        return;
    }
    const kind = kindField.value as DelayKind;
    try {
        const flight = await addDelay(flightId, {
            kind,
            minutes: Number(minutesField.value),
            reason: reasonField.value,
            description: descriptionField.value.trim(),
        });
        delayForm.reset();
        renderFlight(flight);
        addInfo(messageBox, DELAY_ADDED[kind]);
    } catch (error) {
        addError(messageBox, (error as Error).message);
    }
}

async function init(): Promise<void> {
    delayForm.addEventListener("submit", enterDelay);
    if (!flightId) {
        addError(messageBox, "Geen vlucht gekozen: open deze pagina met ?flightId=… in het adres.");
        return;
    }
    const role = await plannerRoleOrNoAccess(messageBox);
    if (role === null) {
        return;
    }
    isFlightPlanner = role === FLIGHT_PLANNER;
    try {
        if (isFlightPlanner) {
            renderDelayReasons(await getDelayReasons());
        }
        renderFlight(await getFlight(flightId));
    } catch (error) {
        addError(messageBox, (error as Error).message);
    }
}

init();
