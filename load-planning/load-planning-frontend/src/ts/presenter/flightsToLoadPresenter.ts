import {listQuery, showsEta, showsEtd} from "../domain/FlightToLoad.ts";
import type {FlightsToLoad, FlightToLoad} from "../domain/FlightToLoad.ts";
import {getFlightsToLoad} from "../infrastructure/flightCargoRest.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {startAutoRefresh} from "./autoRefresh.ts";
import {brusselsDateTime, plainDate} from "./brusselsTime.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {addError, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const section = document.getElementById("flights-to-load")!;
const dayField = document.getElementById("day") as HTMLInputElement;
const hideDepartedBox = document.getElementById("hide-departed") as HTMLInputElement;
const flightsTable = document.getElementById("flights-table")!;
const flightRows = document.getElementById("flights")!;
const noFlights = document.getElementById("no-flights")!;

/* The address holds the choice of the user. Without a day in it the page sends none: the server answers with
   today and the date field takes the day from that answer. */
const address = new URLSearchParams(window.location.search);
let day: string | null = address.get("scheduledDepartureDate");
let includeDeparted = address.get("includeDeparted") === "true";

function flightRow(flight: FlightToLoad): HTMLTableRowElement {
    const row = document.createElement("tr");
    const link = createElementWithText("a", flight.flightNumber) as HTMLAnchorElement;
    link.href = `cargo.html?flightCargoId=${encodeURIComponent(flight.id)}`;
    link.className = "link";
    const linkCell = document.createElement("td");
    linkCell.appendChild(link);
    row.appendChild(linkCell);
    const cells = [
        plainDate(flight.scheduledDepartureDate),
        flight.departureAirport,
        flight.arrivalAirport,
        brusselsDateTime(flight.std),
        showsEtd(flight) ? brusselsDateTime(flight.etd) : "",
        brusselsDateTime(flight.sta),
        showsEta(flight) ? brusselsDateTime(flight.eta) : "",
        flight.departed ? "Opgestegen" : "",
        flight.finalized ? "Gefinaliseerd" : "",
    ];
    for (const cell of cells) {
        row.appendChild(createElementWithText("td", cell));
    }
    return row;
}

function render(flightsToLoad: FlightsToLoad): void {
    day = flightsToLoad.scheduledDepartureDate;
    dayField.value = day;
    hideDepartedBox.checked = !includeDeparted;
    const flights = flightsToLoad.flights;
    flightRows.replaceChildren(...flights.map(flightRow));
    flightsTable.classList.toggle("hidden", flights.length === 0);
    noFlights.classList.toggle("hidden", flights.length > 0);
    noFlights.textContent = `Geen te beladen vluchten op ${plainDate(day)}.`
        + (includeDeparted ? "" : " Opgestegen vluchten zijn verborgen.");
    section.classList.remove("hidden");
}

/* Asks the list again; on failure what is on the screen stays and the reason is shown. */
async function reload(): Promise<boolean> {
    try {
        render(await getFlightsToLoad(day, includeDeparted));
        return true;
    } catch (error) {
        clearMessages(messageBox);
        addError(messageBox, (error as Error).message);
        return false;
    }
}

/* A new choice goes into the address and the list is asked again. */
async function choose(): Promise<void> {
    clearMessages(messageBox);
    day = dayField.value || null;
    includeDeparted = !hideDepartedBox.checked;
    if (await reload() && day) {
        window.history.replaceState(null, "", listQuery(day, includeDeparted));
    }
}

async function init(): Promise<void> {
    if (await plannerRoleOrNoAccess(messageBox) === null) {
        return;
    }
    dayField.addEventListener("change", choose);
    hideDepartedBox.addEventListener("change", choose);
    startAutoRefresh(reload, () => true);
}

init();
