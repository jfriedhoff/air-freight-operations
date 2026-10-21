import {deckCells, keptPick, offloadingReason, sideWeights} from "../domain/Deck.ts";
import type {DeckCell, Pick} from "../domain/Deck.ts";
import {canChangeLoad, holderOf, stateOf, withoutLoadPosition} from "../domain/FlightCargo.ts";
import type {FlightCargo, Uld} from "../domain/FlightCargo.ts";
import {identityIn, identityQuery} from "../domain/FlightIdentity.ts";
import type {FlightIdentity} from "../domain/FlightIdentity.ts";
import type {LoadCheckReport} from "../domain/LoadCheckReport.ts";
import {listQuery, showsEta, showsEtd} from "../domain/FlightToLoad.ts";
import {flightOperationsAddress} from "../infrastructure/contexts.ts";
import {
    assignLoadPosition, checkLoad, clearLoadPosition, finalizeLoad, findFlightCargo, getFlightCargo,
} from "../infrastructure/flightCargoRest.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {startAutoRefresh} from "./autoRefresh.ts";
import {brusselsDateTime, plainDate} from "./brusselsTime.ts";
import {drawDeck, drawFindings, drawPick, drawTray, focusFirstTarget} from "./deckPresenter.ts";
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
const checkLoadButton = document.getElementById("check-load-btn") as HTMLButtonElement;

/* The page opens with the id of the cargo, or with the identity of the flight when Vluchtdetails links to it. */
const address = new URLSearchParams(window.location.search);
let flightCargoId: string | null = address.get("flightCargoId");
const identity: FlightIdentity | null = identityIn(address);
let isLoadPlanner = false;
/* A request to assign or clear a load position is on its way: the automatic reload waits for it. */
let assigning = false;
/* A request to finalize is on its way: the automatic reload waits for it too. */
let finalizing = false;
/* The number of the latest load of the cargo (a reload, or the answer to a change). An answer to an earlier one
   that arrives later is not drawn: it would put an older state over a newer one. */
let latestLoad = 0;
/* What the deck shows now: the cargo of the latest load and the report of its checks (null without one). */
let shown: {cargo: FlightCargo, report: LoadCheckReport | null} | null = null;
/* The ULD the load planner picked to place or move, or null. */
let pick: Pick | null = null;
/* Why the checks of the latest load could not be asked, or null when they could (or were not needed). */
let checksFailure: string | null = null;

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

/* The list of the floor plan's positions next to a ULD, as the case asks; the deck above is the main way. A choice
   with the mouse is sent at once; a choice with the keyboard (arrow keys, typing) only on Enter or when the list is
   left, because in some browsers every arrow key changes a closed list. "Leegmaken" takes the ULD off its position.
   A position held by another ULD is shown with it and cannot be chosen; the server decides anyway. */
function positionChoice(cargo: FlightCargo, uld: Uld): HTMLElement {
    const cell = document.createElement("td");
    const choice = document.createElement("select");
    choice.className = "select select-sm w-auto";
    choice.setAttribute("aria-label", `Laadpositie van ${uld.code}`);
    choice.dataset.uld = uld.code;
    const none = createElementWithText("option", uld.loadPosition === null ? "Kies een laadpositie" : "Leegmaken") as
        HTMLOptionElement;
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
    const stored = uld.loadPosition ?? "";
    choice.value = stored;
    let byKeyboard = false;
    const send = (): void => {
        byKeyboard = false;
        if (choice.value === stored) {
            return;
        }
        if (choice.value === "") {
            clear(cargo.id, uld.code);
        } else {
            assign(cargo.id, uld.code, choice.value);
        }
    };
    choice.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            send();
        } else if (event.key !== "Tab") {
            byKeyboard = true;
        }
    });
    choice.addEventListener("change", () => {
        if (!byKeyboard) {
            send();
        }
    });
    choice.addEventListener("blur", () => {
        if (byKeyboard) {
            send();
        }
    });
    cell.appendChild(choice);
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
    text("deck-arrival", cargo.arrivalAirport);
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
    // the table is drawn anew: the keyboard stays on the list of the same ULD
    const focused = document.activeElement instanceof HTMLSelectElement && uldRows.contains(document.activeElement)
        ? document.activeElement.dataset.uld : undefined;
    uldRows.replaceChildren(...cargo.ulds.map(uld => uldRow(cargo, uld, changeable)));
    if (focused !== undefined) {
        uldRows.querySelector<HTMLSelectElement>(`select[data-uld="${focused}"]`)?.focus();
    }
    loadActions.classList.toggle("hidden", !changeable);
    uldSummary.textContent = summary(cargo);
    noUlds.classList.toggle("hidden", cargo.ulds.length > 0);
    uldTable.classList.toggle("hidden", cargo.ulds.length === 0);
    section.classList.remove("hidden");
}

/* The deck with what the checks say about the load as it stands. Only while the load planner can change the load:
   then the page asks the checks after every load, and the report below shows the same answer. Otherwise the deck is
   only drawn, and the report keeps what it last showed (the report of Finalize Load). */
async function showDeck(cargo: FlightCargo, load: number): Promise<void> {
    checksFailure = null;
    if (!canChangeLoad(cargo, isLoadPlanner)) {
        redrawDeck(cargo, null);
        drawFindings(null, null, null);
        return;
    }
    try {
        const report = await checkLoad(cargo.id);
        if (load !== latestLoad) {
            return;
        }
        redrawDeck(cargo, report);
        drawFindings(sideWeights(report), offloadingReason(report), null);
        showReport(report);
    } catch (error) {
        if (load === latestLoad) {
            checksFailure = (error as Error).message;
            redrawDeck(cargo, null);
            drawFindings(null, null,
                "De controles konden niet opgehaald worden; het vloerplan toont de lading zonder.");
        }
    }
}

/* Deck, tray and the line about the picked ULD, from what the deck shows now. A pick needs no new answer. */
function redrawDeck(cargo: FlightCargo, report: LoadCheckReport | null): void {
    shown = {cargo, report};
    const changeable = canChangeLoad(cargo, isLoadPlanner);
    const cells = deckCells(cargo, report, pick?.code ?? null, changeable);
    drawDeck(cells, cell => changeable && (cell.uld !== null || pick !== null), cell => clickOnDeck(cargo, cell));
    drawTray(cargo, changeable, pick, uld => choose({code: uld.code, position: null}));
    drawPick(pick, () => clear(cargo.id, pick!.code), () => choose(null));
}

/* Picking the picked ULD again drops it; picking another one changes the pick. A ULD picked from the tray puts the
   keyboard on the first position it can go to. */
function choose(next: Pick | null): void {
    pick = next !== null && pick !== null && pick.code === next.code ? null : next;
    if (shown !== null) {
        redrawDeck(shown.cargo, shown.report);
    }
    if (pick !== null && pick.position === null) {
        focusFirstTarget();
    }
}

/* A list next to a ULD that has the focus may hold a keyboard choice that is not sent yet, or be open: the
   automatic reload would redraw it under the user. */
function aListHasTheFocus(): boolean {
    return document.activeElement instanceof HTMLSelectElement && uldRows.contains(document.activeElement);
}

/* A ULD on the deck is picked; a free position takes the picked ULD. */
function clickOnDeck(cargo: FlightCargo, cell: DeckCell): void {
    if (cell.uld !== null) {
        choose({code: cell.uld, position: cell.name});
    } else if (pick !== null) {
        assign(cargo.id, pick.code, cell.name);
    }
}

/* Draws one answer of the server, unless a newer load was started in the meantime. */
async function display(answer: Promise<FlightCargo>): Promise<void> {
    const load = ++latestLoad;
    const cargo = await answer;
    if (load !== latestLoad) {
        return;
    }
    const before = pick;
    pick = keptPick(cargo, pick, canChangeLoad(cargo, isLoadPlanner));
    if (before !== null && pick === null && !assigning) {
        addInfo(messageBox, `${before.code} is intussen verplaatst of de lading kan niet meer wijzigen: kies opnieuw.`);
    }
    render(cargo);
    await showDeck(cargo, load);
}

/* Asks the cargo again; on failure what is on the screen stays and the reason is shown. A message the user
   has not acted on yet stays too: a reload never clears the message box when it succeeds. */
async function reload(): Promise<boolean> {
    try {
        await display(getFlightCargo(flightCargoId!));
        return true;
    } catch (error) {
        clearMessages(messageBox);
        addError(messageBox, (error as Error).message);
        return false;
    }
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
    pick = null;
    try {
        await display(change());
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

/* Check Load: the checks run again on the load as it is stored now, and the report shows them. */
async function checkTheLoad(): Promise<void> {
    clearMessages(messageBox);
    if (!await reload()) {
        return;
    }
    if (checksFailure !== null) {
        addError(messageBox, checksFailure);
    } else {
        addInfo(messageBox, "De lading is gecontroleerd: zie het rapport.");
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
    checkLoadButton.addEventListener("click", checkTheLoad);
    document.addEventListener("keydown", event => {
        const typing = event.target instanceof HTMLInputElement || event.target instanceof HTMLSelectElement;
        if (event.key === "Escape" && pick !== null && !typing) {
            choose(null);
        }
    });
    startAutoRefresh(reload, () => !assigning && !finalizing && !aListHasTheFocus());
}

init();
