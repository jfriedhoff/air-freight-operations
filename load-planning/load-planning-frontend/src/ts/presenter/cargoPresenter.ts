import {canChangeLoad, holderOf, stateOf, withoutLoadPosition} from "../domain/FlightCargo.ts";
import type {FlightCargo, Uld} from "../domain/FlightCargo.ts";
import {identityIn, identityQuery} from "../domain/FlightIdentity.ts";
import type {FlightIdentity} from "../domain/FlightIdentity.ts";
import {listQuery, showsEta, showsEtd} from "../domain/FlightToLoad.ts";
import {flightOperationsAddress} from "../infrastructure/contexts.ts";
import {
    assignLoadPosition, clearLoadPosition, finalizeLoad, findFlightCargo, getFlightCargo,
} from "../infrastructure/flightCargoRest.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {startAutoRefresh} from "./autoRefresh.ts";
import {brusselsDateTime, plainDate} from "./brusselsTime.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {showReport} from "./loadCheckReportPresenter.ts";
import {addError, addInfo, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const section = document.getElementById("cargo")!;
const backToList = document.getElementById("back-to-list") as HTMLAnchorElement;
const flightDetailsLink = document.getElementById("flight-details-link") as HTMLAnchorElement;
const loadState = document.getElementById("load-state")!;
const roleNote = document.getElementById("role-note")!;
const uldSummary = document.getElementById("uld-summary")!;
const noUlds = document.getElementById("no-ulds")!;
const uldTable = document.getElementById("uld-table")!;
const uldRows = document.getElementById("ulds")!;
const loadActions = document.getElementById("load-actions")!;
const finalizeButton = document.getElementById("finalize-btn") as HTMLButtonElement;

/* The page opens with the id of the cargo, or with the identity of the flight when Vluchtdetails links to it. */
const address = new URLSearchParams(window.location.search);
let flightCargoId: string | null = address.get("flightCargoId");
const identity: FlightIdentity | null = identityIn(address);
let isLoadPlanner = false;
/* A request to assign or clear a load position is on its way: the automatic reload waits for it. */
let assigning = false;
/* A request to finalize is on its way: the automatic reload waits for it too. */
let finalizing = false;

function text(id: string, value: string): void {
    document.getElementById(id)!.textContent = value;
}

/* Shows the row of an estimated time only when it differs from the scheduled time. */
function estimatedTime(rowId: string, valueId: string, instant: string, shown: boolean): void {
    document.getElementById(rowId)!.classList.toggle("hidden", !shown);
    text(valueId, brusselsDateTime(instant));
}

function weight(uld: Uld): string {
    return `${uld.weight.value} ${uld.weight.unit.toLowerCase()}`;
}

/* The words for why the load cannot change. The server decided that; see stateOf. */
function stateLine(cargo: FlightCargo, loadedAt: Date): string {
    const start = brusselsDateTime(cargo.loadingPeriod.start);
    const end = brusselsDateTime(cargo.loadingPeriod.end);
    switch (stateOf(cargo, loadedAt)) {
        case "FINALIZED":
            return cargo.finalizedBy === null
                ? "De lading is gefinaliseerd: laadposities kunnen niet meer wijzigen."
                : `De lading is gefinaliseerd door ${cargo.finalizedBy}: laadposities kunnen niet meer wijzigen.`;
        case "DEPARTED":
            return "De vlucht is opgestegen.";
        case "NOT_STARTED":
            return `De beladingsperiode begint op ${start}.`;
        case "ENDED":
            return `De beladingsperiode is afgelopen sinds ${end}.`;
        case "OUTSIDE_PERIOD":
            return `Buiten de beladingsperiode (van ${start} tot ${end}).`;
        case "OPEN":
            return "";
    }
}

/* The selection list holds the load positions of the floor plan, as the server gives them, and a button:
   a choice is only sent when the load planner says so. A position that holds another ULD is shown with that ULD
   and cannot be chosen; the server decides anyway. A ULD on a position also gets a button to clear it. */
function positionChoice(cargo: FlightCargo, uld: Uld): HTMLElement {
    const cell = document.createElement("td");
    const choice = document.createElement("select");
    choice.className = "select select-sm w-auto";
    choice.dataset.current = uld.loadPosition ?? "";
    const none = createElementWithText("option", "Kies een laadpositie") as HTMLOptionElement;
    none.value = "";
    choice.appendChild(none);
    for (const position of cargo.loadPositions) {
        const holder = holderOf(cargo, position);
        const takenByAnother = holder !== null && holder !== uld.code;
        const option = createElementWithText("option", takenByAnother ? `${position} (${holder})` : position) as
            HTMLOptionElement;
        option.value = position;
        option.disabled = takenByAnother;
        choice.appendChild(option);
    }
    choice.value = uld.loadPosition ?? "";
    const assignButton = createElementWithText("button", "Toekennen") as HTMLButtonElement;
    assignButton.className = "btn btn-sm btn-outline ml-2";
    assignButton.addEventListener("click", () => assign(cargo.id, uld.code, choice.value));
    cell.append(choice, assignButton);
    if (uld.loadPosition !== null) {
        const clearButton = createElementWithText("button", "Leegmaken") as HTMLButtonElement;
        clearButton.className = "btn btn-sm ml-2";
        clearButton.addEventListener("click", () => clear(cargo.id, uld.code));
        cell.appendChild(clearButton);
    }
    return cell;
}

function uldRow(cargo: FlightCargo, uld: Uld, changeable: boolean): HTMLTableRowElement {
    const row = document.createElement("tr");
    if (uld.loadPosition === null) {
        row.className = "bg-warning/20";
    }
    for (const cell of [uld.code, weight(uld), uld.destination]) {
        row.appendChild(createElementWithText("td", cell));
    }
    row.appendChild(changeable ? positionChoice(cargo, uld) : createElementWithText("td", uld.loadPosition ?? "–"));
    // only next to a load position: after a ULD is taken off, the name would say who took it off
    row.appendChild(createElementWithText("td", uld.loadPosition === null ? "–" : uld.positionChangedBy ?? "–"));
    return row;
}

function summary(cargo: FlightCargo): string {
    const total = cargo.ulds.length;
    const without = withoutLoadPosition(cargo).length;
    if (total === 0) {
        return "";
    }
    return without === 0
        ? `Alle ${total} ULD's hebben een laadpositie.`
        : `${without} van ${total} ULD's zonder laadpositie.`;
}

function render(cargo: FlightCargo): void {
    const loadedAt = new Date();
    text("flight-number", cargo.flightNumber);
    text("scheduled-departure-date", plainDate(cargo.scheduledDepartureDate));
    text("departure-airport", cargo.departureAirport);
    text("arrival-airport", cargo.arrivalAirport);
    text("std", brusselsDateTime(cargo.std));
    text("sta", brusselsDateTime(cargo.sta));
    estimatedTime("etd-row", "etd", cargo.etd, showsEtd(cargo));
    estimatedTime("eta-row", "eta", cargo.eta, showsEta(cargo));
    text("loading-period", `van ${brusselsDateTime(cargo.loadingPeriod.start)} tot `
        + brusselsDateTime(cargo.loadingPeriod.end));

    backToList.href = "flights-to-load.html" + listQuery(cargo.scheduledDepartureDate, cargo.departed);
    backToList.textContent = `← Te beladen vluchten van ${plainDate(cargo.scheduledDepartureDate)}`;
    flightDetailsLink.href = flightOperationsAddress("html/flight-details.html" + identityQuery({
        flightNumber: cargo.flightNumber,
        scheduledDepartureDate: cargo.scheduledDepartureDate,
        departureAirport: cargo.departureAirport,
    }));

    const state = stateLine(cargo, loadedAt);
    loadState.textContent = state;
    loadState.classList.toggle("hidden", state === "");
    roleNote.classList.toggle("hidden", isLoadPlanner);

    const changeable = canChangeLoad(cargo, isLoadPlanner);
    uldRows.replaceChildren(...cargo.ulds.map(uld => uldRow(cargo, uld, changeable)));
    loadActions.classList.toggle("hidden", !changeable);
    uldSummary.textContent = summary(cargo);
    noUlds.classList.toggle("hidden", cargo.ulds.length > 0);
    uldTable.classList.toggle("hidden", cargo.ulds.length === 0);
    section.classList.remove("hidden");
}

/* Asks the cargo again; on failure what is on the screen stays and the reason is shown. A message the user
   has not acted on yet stays too: a reload never clears the message box when it succeeds. */
async function reload(): Promise<boolean> {
    try {
        render(await getFlightCargo(flightCargoId!));
        return true;
    } catch (error) {
        clearMessages(messageBox);
        addError(messageBox, (error as Error).message);
        return false;
    }
}

/* A selection list that shows another position than the stored one: a choice in progress. */
function aChoiceIsInProgress(): boolean {
    return Array.from(uldRows.querySelectorAll("select")).some(choice => choice.value !== choice.dataset.current);
}

/* Whether the choice is allowed is the server's answer. */
async function assign(cargoId: string, uldCode: string, loadPosition: string): Promise<void> {
    clearMessages(messageBox);
    if (loadPosition === "") {
        addError(messageBox, "Kies eerst een laadpositie.");
        return;
    }
    await changeLoadPosition(() => assignLoadPosition(cargoId, uldCode, loadPosition),
        `${uldCode} staat op ${loadPosition}.`);
}

async function clear(cargoId: string, uldCode: string): Promise<void> {
    clearMessages(messageBox);
    await changeLoadPosition(() => clearLoadPosition(cargoId, uldCode), `${uldCode} heeft geen laadpositie meer.`);
}

/* The page redraws from the server's answer. After a refusal the cargo is asked again, so the page shows what is
   stored, and the reason of the refusal stays on the screen. */
async function changeLoadPosition(change: () => Promise<FlightCargo>, confirmation: string): Promise<void> {
    assigning = true;
    try {
        render(await change());
        addInfo(messageBox, confirmation);
    } catch (error) {
        const refusal = (error as Error).message;
        await reload();
        clearMessages(messageBox);
        addError(messageBox, refusal);
    } finally {
        assigning = false;
    }
}

/* The server runs the checks again and finalizes only when every hard check passes; either way it sends the
   report, which the page shows. A refusal for another reason, such as the loading period, is shown as an error. */
async function finalize(): Promise<void> {
    // Finalizing cannot be undone: the CPM leaves for the partners and the load positions are frozen.
    if (!window.confirm("Na het finaliseren kunnen de laadposities niet meer wijzigen en wordt de CPM verstuurd. "
        + "Lading finaliseren?")) {
        return;
    }
    clearMessages(messageBox);
    finalizing = true;
    finalizeButton.disabled = true;
    try {
        const outcome = await finalizeLoad(flightCargoId!);
        showReport(outcome.report);
        await reload();
        if (outcome.finalized) {
            addInfo(messageBox, "De lading is gefinaliseerd.");
        } else {
            addError(messageBox, "De lading is niet gefinaliseerd: een hard check faalt.");
        }
    } catch (error) {
        const refusal = (error as Error).message;
        await reload();
        clearMessages(messageBox);
        addError(messageBox, refusal);
    } finally {
        finalizing = false;
        finalizeButton.disabled = false;
    }
}

/* The id of the cargo of the flight with this identity. Flight events reach Load Planning through Messaging, so a
   flight that Flight Operations just created may not be known here for a moment. */
async function flightCargoIdOf(flight: FlightIdentity): Promise<string | null> {
    try {
        const found = await findFlightCargo(flight);
        if (found === null) {
            addError(messageBox, `Load Planning kent vlucht ${flight.flightNumber} van `
                + `${plainDate(flight.scheduledDepartureDate)} uit ${flight.departureAirport} nog niet. `
                + "Probeer het zo meteen opnieuw.");
            return null;
        }
        window.history.replaceState(null, "", `?flightCargoId=${encodeURIComponent(found.id)}`);
        return found.id;
    } catch (error) {
        addError(messageBox, (error as Error).message);
        return null;
    }
}

async function init(): Promise<void> {
    const role = await plannerRoleOrNoAccess(messageBox);
    if (role === null) {
        return;
    }
    isLoadPlanner = role === "load-planner";
    if (!flightCargoId && !identity) {
        addError(messageBox, "Kies een vlucht in de lijst van te beladen vluchten.");
        return;
    }
    if (!flightCargoId) {
        flightCargoId = await flightCargoIdOf(identity!);
        if (!flightCargoId) {
            return;
        }
    }
    finalizeButton.addEventListener("click", finalize);
    startAutoRefresh(reload, () => !assigning && !finalizing && !aChoiceIsInProgress());
}

init();
