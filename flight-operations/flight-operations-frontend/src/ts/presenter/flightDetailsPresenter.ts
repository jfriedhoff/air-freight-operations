import {canChangeDelay, canRegisterAta, canRegisterAtd, delayKindsToAdd, showsEta, showsEtd} from "../domain/Flight.ts";
import type {Airport, Delay, DelayKind, DelayReason, Flight} from "../domain/Flight.ts";
import {identityIn, identityQuery} from "../domain/FlightIdentity.ts";
import type {FlightIdentity} from "../domain/FlightIdentity.ts";
import {overviewQuery} from "../domain/FlightOverview.ts";
import {FLIGHT_PLANNER} from "../infrastructure/auth.ts";
import {loadPlanningAddress} from "../infrastructure/contexts.ts";
import {
    addDelay, changeDelay, findFlight, getDelayReasons, getFlight, registerArrival, registerDeparture, removeDelay
} from "../infrastructure/flightRest.ts";
import {brusselsDate, brusselsDateTime, brusselsToUtc, plainDate} from "./brusselsTime.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {addError, addInfo, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const backToOverview = document.getElementById("back-to-overview") as HTMLAnchorElement;
const cargoLink = document.getElementById("cargo-link") as HTMLAnchorElement;
const details = document.getElementById("flight-details")!;
const delayRows = document.getElementById("delays")!;
const noDelays = document.getElementById("no-delays")!;
const delayForm = document.getElementById("delay-form") as HTMLFormElement;
const kindField = document.getElementById("kind") as HTMLSelectElement;
const minutesField = document.getElementById("minutes") as HTMLInputElement;
const reasonField = document.getElementById("reason") as HTMLSelectElement;
const descriptionField = document.getElementById("description") as HTMLTextAreaElement;

/* A form that asks for one moment in Brussels time: the ATD form and the ATA form. */
interface MomentForm {
    form: HTMLFormElement;
    date: HTMLInputElement;
    time: HTMLInputElement;
    zoneChoice: HTMLElement;
    zone: HTMLSelectElement;
}

function momentForm(prefix: string): MomentForm {
    return {
        form: document.getElementById(`${prefix}-form`) as HTMLFormElement,
        date: document.getElementById(`${prefix}-date`) as HTMLInputElement,
        time: document.getElementById(`${prefix}-time`) as HTMLInputElement,
        zoneChoice: document.getElementById(`${prefix}-zone-choice`)!,
        zone: document.getElementById(`${prefix}-zone`) as HTMLSelectElement,
    };
}

const atdForm = momentForm("atd");
const ataForm = momentForm("ata");

const DELAY_KIND = {DEPARTURE: "Vertrek", ARRIVAL: "Aankomst"};
const DELAY_ADDED = {
    DEPARTURE: "De vertrekvertraging is geregistreerd.",
    ARRIVAL: "De aankomstvertraging is geregistreerd."
};
const STATUS = {PLANNED: "Gepland", DEPARTED: "Opgestegen", LANDED: "Geland"};

/* The page opens with the id of the flight, or with its identity when Load Planning links to it. */
const address = new URLSearchParams(window.location.search);
let flightId: string | null = address.get("flightId");
const identity: FlightIdentity | null = identityIn(address);
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

/* A registered actual time also shows who registered it. */
function actualTime(rowId: string, valueId: string, instant: string | null, registeredBy: string | null): void {
    optionalTime(rowId, valueId, instant, true);
    if (instant !== null && registeredBy !== null) {
        text(valueId, `${brusselsDateTime(instant)} (door ${registeredBy})`);
    }
}

function reasonText(delay: Delay): string {
    return delay.reason.description ? `${delay.reason.code} – ${delay.reason.description}` : delay.reason.code;
}

function button(label: string, classes: string, onClick: () => void): HTMLElement {
    const element = createElementWithText("button", label);
    element.setAttribute("type", "button");
    element.className = `btn btn-xs ${classes}`;
    element.addEventListener("click", onClick);
    return element;
}

function cellWith(...elements: HTMLElement[]): HTMLElement {
    const cell = document.createElement("td");
    cell.append(...elements);
    return cell;
}

/* A flight planner gets the buttons only for a delay that can still change; see canChangeDelay. */
function delayRow(flight: Flight, delay: Delay): HTMLTableRowElement {
    const row = document.createElement("tr");
    for (const cell of [DELAY_KIND[delay.kind], `${delay.minutes} min`, reasonText(delay), delay.description,
        delay.lastChangedBy ?? "–"]) {
        row.appendChild(createElementWithText("td", cell));
    }
    row.appendChild(canChangeDelay(flight, delay, isFlightPlanner)
        ? cellWith(
            button("Wijzigen", "", () => row.replaceWith(delayEditRow(flight, delay))),
            button("Verwijderen", "btn-ghost", () => deleteDelay(delay)))
        : cellWith());
    return row;
}

/* The kind and the delay reason of a delay stay: the row only lets the planner change the minutes and the
   description. Annuleren puts the row back as it was. */
function delayEditRow(flight: Flight, delay: Delay): HTMLTableRowElement {
    const minutes = document.createElement("input");
    minutes.type = "number";
    minutes.required = true;
    minutes.value = String(delay.minutes);
    minutes.className = "input input-xs w-20";
    const description = document.createElement("input");
    description.required = true;
    // the same maximum as the add form: Flight Operations refuses a longer description
    description.maxLength = 255;
    description.value = delay.description;
    description.className = "input input-xs w-full";

    const row = document.createElement("tr");
    row.append(createElementWithText("td", DELAY_KIND[delay.kind]), cellWith(minutes),
        createElementWithText("td", reasonText(delay)), cellWith(description),
        createElementWithText("td", delay.lastChangedBy ?? "–"),
        cellWith(
            button("Opslaan", "btn-primary", () => saveDelay(delay, minutes, description)),
            button("Annuleren", "btn-ghost", () => row.replaceWith(delayRow(flight, delay)))));
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
    cargoLink.href = loadPlanningAddress("html/cargo.html" + identityQuery({
        flightNumber: flight.flightNumber,
        scheduledDepartureDate: flight.scheduledDepartureDate,
        departureAirport: flight.departureAirport.code,
    }));
    text("flight-number", flight.flightNumber);
    text("scheduled-departure-date", plainDate(flight.scheduledDepartureDate));
    text("status", STATUS[flight.status]);
    text("departure-airport", airport(flight.departureAirport));
    text("arrival-airport", airport(flight.arrivalAirport));
    text("std", brusselsDateTime(flight.std));
    text("sta", brusselsDateTime(flight.sta));
    optionalTime("etd-row", "etd", flight.etd, showsEtd(flight));
    optionalTime("eta-row", "eta", flight.eta, showsEta(flight));
    actualTime("atd-row", "atd", flight.atd, flight.atdRegisteredBy);
    actualTime("ata-row", "ata", flight.ata, flight.ataRegisteredBy);

    delayRows.replaceChildren(...flight.delays.map(delay => delayRow(flight, delay)));
    noDelays.classList.toggle("hidden", flight.delays.length > 0);
    const kinds = delayKindsToAdd(flight, isFlightPlanner);
    kindField.replaceChildren(...kinds.map(kindOption));
    delayForm.classList.toggle("hidden", kinds.length === 0);
    showMomentForm(atdForm, canRegisterAtd(flight, isFlightPlanner), flight.etd);
    showMomentForm(ataForm, canRegisterAta(flight, isFlightPlanner), flight.eta);
    // only with a flight on the screen: a load planner reads why there is no form
    document.getElementById("role-note")!.classList.toggle("hidden", isFlightPlanner);
    details.classList.remove("hidden");
}

/* The date starts at the estimated day, so the planner usually only types the time. */
function showMomentForm(fields: MomentForm, shown: boolean, estimated: string): void {
    fields.form.classList.toggle("hidden", !shown);
    if (!fields.date.value) {
        fields.date.value = brusselsDate(estimated);
    }
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

/* Like the delay form, the row only checks that something was entered; whether the change is allowed is the
   server's answer. */
async function saveDelay(delay: Delay, minutes: HTMLInputElement, description: HTMLInputElement): Promise<void> {
    clearMessages(messageBox);
    if (!flightId || !minutes.reportValidity() || !description.reportValidity()) {
        return;
    }
    try {
        renderFlight(await changeDelay(flightId, delay.id, {
            minutes: Number(minutes.value),
            description: description.value.trim(),
        }));
        addInfo(messageBox, "De vertraging is gewijzigd.");
    } catch (error) {
        addError(messageBox, (error as Error).message);
    }
}

async function deleteDelay(delay: Delay): Promise<void> {
    clearMessages(messageBox);
    const kind = DELAY_KIND[delay.kind].toLowerCase();
    if (!flightId || !window.confirm(`De ${kind}vertraging van ${delay.minutes} min verwijderen?`)) {
        return;
    }
    try {
        renderFlight(await removeDelay(flightId, delay.id));
        addInfo(messageBox, "De vertraging is verwijderd.");
    } catch (error) {
        addError(messageBox, (error as Error).message);
    }
}

function zoneOption(label: string, moment: Date): HTMLElement {
    const option = createElementWithText("option", label);
    option.setAttribute("value", moment.toISOString());
    return option;
}

/* "00:30", the UTC time of a moment */
function utcTime(moment: Date): string {
    return moment.toISOString().slice(11, 16);
}

/* Which UTC moment the typed Brussels time means; null while the page still needs an answer from the planner. */
function typedMoment(fields: MomentForm): Date | null {
    const date = fields.date.value;
    const time = fields.time.value;
    const input = brusselsToUtc(date, time);
    if (input.kind === "nonexistent") {
        addError(messageBox, `${time} bestaat niet in Brussel op ${plainDate(date)}: de klok springt dan van 02:00 naar 03:00.`);
        return null;
    }
    if (input.kind === "exact") {
        return input.utc;
    }
    if (fields.zoneChoice.classList.contains("hidden")) {
        fields.zone.replaceChildren(
            zoneOption(`${time} zomertijd (${utcTime(input.summer)} UTC)`, input.summer),
            zoneOption(`${time} wintertijd (${utcTime(input.winter)} UTC)`, input.winter));
        fields.zoneChoice.classList.remove("hidden");
        addInfo(messageBox, `${time} bestaat twee keer in Brussel op ${plainDate(date)}. Kies welk tijdstip je bedoelt.`);
        return null;
    }
    return new Date(fields.zone.value);
}

/* Whether the time is allowed is the server's answer: the page only converts the Brussels time to UTC. */
async function registerActualTime(event: Event, fields: MomentForm,
                                  register: (flightId: string, moment: Date) => Promise<Flight>,
                                  registered: string): Promise<void> {
    event.preventDefault();
    clearMessages(messageBox);
    if (!flightId || !fields.form.reportValidity()) {
        return;
    }
    const moment = typedMoment(fields);
    if (moment === null) {
        return;
    }
    try {
        const flight = await register(flightId, moment);
        fields.form.reset();
        fields.zoneChoice.classList.add("hidden");
        renderFlight(flight);
        addInfo(messageBox, registered);
    } catch (error) {
        addError(messageBox, (error as Error).message);
    }
}

/* The id of the flight with this identity. The address then holds the id, so a reload asks the flight directly. */
async function flightIdOf(flight: FlightIdentity): Promise<string | null> {
    const found = await findFlight(flight);
    if (found === null) {
        addError(messageBox, `Vlucht ${flight.flightNumber} van ${plainDate(flight.scheduledDepartureDate)} uit `
            + `${flight.departureAirport} bestaat niet (meer).`);
        return null;
    }
    window.history.replaceState(null, "", `?flightId=${encodeURIComponent(found.id)}`);
    return found.id;
}

async function init(): Promise<void> {
    delayForm.addEventListener("submit", enterDelay);
    atdForm.form.addEventListener("submit", event =>
        registerActualTime(event, atdForm, registerDeparture, "Het vertrek is geregistreerd."));
    ataForm.form.addEventListener("submit", event =>
        registerActualTime(event, ataForm, registerArrival, "De aankomst is geregistreerd."));
    // a new date or time needs a new answer to "which moment do you mean"
    for (const fields of [atdForm, ataForm]) {
        for (const field of [fields.date, fields.time]) {
            field.addEventListener("input", () => fields.zoneChoice.classList.add("hidden"));
        }
    }
    if (!flightId && !identity) {
        addError(messageBox, "Geen vlucht gekozen: kies een vlucht in het overzicht van de vluchten.");
        return;
    }
    const role = await plannerRoleOrNoAccess(messageBox);
    if (role === null) {
        return;
    }
    isFlightPlanner = role === FLIGHT_PLANNER;
    try {
        if (!flightId) {
            flightId = await flightIdOf(identity!);
            if (!flightId) {
                return;
            }
        }
        if (isFlightPlanner) {
            renderDelayReasons(await getDelayReasons());
        }
        renderFlight(await getFlight(flightId));
    } catch (error) {
        addError(messageBox, (error as Error).message);
    }
}

init();