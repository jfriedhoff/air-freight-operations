import {listQuery, showsEta, showsEtd} from "../domain/FlightToLoad.ts";
import type {FlightsToLoad, FlightToLoad} from "../domain/FlightToLoad.ts";
import {getFlightsToLoad} from "../infrastructure/flightCargoRest.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {startAutoRefresh} from "./autoRefresh.ts";
import {addDays, brusselsDateTime, longDate, plainDate} from "./brusselsTime.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {addError, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const section = document.getElementById("flights-to-load")!;
const dayField = document.getElementById("day") as HTMLInputElement;
const previousDayButton = document.getElementById("previous-day") as HTMLButtonElement;
const nextDayButton = document.getElementById("next-day") as HTMLButtonElement;
const dayTitle = document.getElementById("day-title")!;
const flightCount = document.getElementById("flight-count")!;
const hideDepartedBox = document.getElementById("hide-departed") as HTMLInputElement;
const flightsTable = document.getElementById("flights-table")!;
const flightRows = document.getElementById("flights")!;
const noFlights = document.getElementById("no-flights")!;

/* The address holds the choice of the user. Without a day in it the page sends none: the server answers with
   today and the date field takes the day from that answer. */
const address = new URLSearchParams(window.location.search);
let day: string | null = address.get("scheduledDepartureDate");
let includeDeparted = address.get("includeDeparted") === "true";
/* The number of the latest request for the list (a choice or an automatic reload), as on the cargo page.
   Only the answer to that request is drawn: an earlier answer that arrives later would put an older list back. */
let latestLoad = 0;

/* A status as a small label, or an empty cell when the flight does not have that status. */
function badgeCell(shown: boolean, text: string, className: string): HTMLTableCellElement {
    const cell = document.createElement("td");
    if (shown) {
        const badge = createElementWithText("span", text);
        badge.className = `badge badge-sm ${className}`;
        cell.appendChild(badge);
    }
    return cell;
}

function flightRow(flight: FlightToLoad): HTMLTableRowElement {
    const row = document.createElement("tr");
    const link = createElementWithText("a", flight.flightNumber) as HTMLAnchorElement;
    link.href = `cargo.html?flightCargoId=${encodeURIComponent(flight.id)}`;
    link.className = "link link-hover font-medium";
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
    ];
    for (const cell of cells) {
        row.appendChild(createElementWithText("td", cell));
    }
    row.append(
        badgeCell(flight.departed, "Opgestegen", "badge-ghost"),
        badgeCell(flight.finalized, "Gefinaliseerd", "badge-soft badge-success"));
    return row;
}

function render(flightsToLoad: FlightsToLoad): void {
    day = flightsToLoad.scheduledDepartureDate;
    dayField.value = day;
    hideDepartedBox.checked = !includeDeparted;
    const flights = flightsToLoad.flights;
    dayTitle.textContent = `Te beladen vluchten van ${longDate(day)}`;
    flightCount.textContent = flights.length === 1 ? "1 vlucht" : `${flights.length} vluchten`;
    flightRows.replaceChildren(...flights.map(flightRow));
    flightsTable.classList.toggle("hidden", flights.length === 0);
    noFlights.classList.toggle("hidden", flights.length > 0);
    noFlights.textContent = `Geen te beladen vluchten op ${plainDate(day)}.`
        + (includeDeparted ? "" : " Opgestegen vluchten zijn verborgen.");
    section.classList.remove("hidden");
}

/* Asks the list again; on failure what is on the screen stays and the reason is shown. Only the answer to the
   latest request is drawn (latestLoad), and the address follows the list that is drawn: a choice whose answer is
   overtaken by a newer automatic reload of the same day still ends with that day in the address. */
async function reload(): Promise<boolean> {
    const load = ++latestLoad;
    try {
        const flightsToLoad = await getFlightsToLoad(day, includeDeparted);
        if (load !== latestLoad) {
            return false;
        }
        render(flightsToLoad);
        if (day) {
            window.history.replaceState(null, "", listQuery(day, includeDeparted));
        }
        return true;
    } catch (error) {
        if (load === latestLoad) {
            clearMessages(messageBox);
            addError(messageBox, (error as Error).message);
        }
        return false;
    }
}

/* A new choice goes into the address and the list is asked again. */
async function choose(): Promise<void> {
    clearMessages(messageBox);
    day = dayField.value || null;
    includeDeparted = !hideDepartedBox.checked;
    await reload();
}

function moveDays(days: number): void {
    if (day) {
        dayField.value = addDays(day, days);
        choose();
    }
}

async function init(): Promise<void> {
    if (await plannerRoleOrNoAccess(messageBox) === null) {
        return;
    }
    dayField.addEventListener("change", choose);
    hideDepartedBox.addEventListener("change", choose);
    previousDayButton.addEventListener("click", () => moveDays(-1));
    nextDayButton.addEventListener("click", () => moveDays(1));
    startAutoRefresh(reload, () => true);
}

init();
