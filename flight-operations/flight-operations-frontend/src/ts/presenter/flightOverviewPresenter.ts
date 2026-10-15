import {overviewQuery} from "../domain/FlightOverview.ts";
import type {FlightRow, FlightsOfDay} from "../domain/FlightOverview.ts";
import {loadPlanningAddress} from "../infrastructure/contexts.ts";
import {getFlights} from "../infrastructure/flightRest.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {startAutoRefresh} from "./autoRefresh.ts";
import {brusselsDateTime, plainDate} from "./brusselsTime.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {addError, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const portal = document.getElementById("portal")!;
const overview = document.getElementById("overview")!;
const flightsToLoadLink = document.getElementById("flights-to-load-link") as HTMLAnchorElement;
const dayField = document.getElementById("day") as HTMLInputElement;
const flightsTable = document.getElementById("flights-table")!;
const flightRows = document.getElementById("flights")!;
const noFlights = document.getElementById("no-flights")!;

const ROLE_NAME = {"flight-planner": "flight planner", "load-planner": "load planner"};

/* The address holds the chosen day. Without a day in it the page sends none: the server answers with today
   (UTC) and the date field takes the day from that answer. The browser never decides what today is. After the
   first answer the page keeps asking that day, so the list does not change day under the user's eyes. */
let day: string | null = new URLSearchParams(window.location.search).get("scheduledDepartureDate");

function flightRow(flight: FlightRow): HTMLTableRowElement {
    const row = document.createElement("tr");
    const link = createElementWithText("a", flight.flightNumber) as HTMLAnchorElement;
    link.href = `html/flight-details.html?flightId=${encodeURIComponent(flight.id)}`;
    link.className = "link";
    const linkCell = document.createElement("td");
    linkCell.appendChild(link);
    row.appendChild(linkCell);
    const cells = [
        plainDate(flight.scheduledDepartureDate),
        flight.departureAirport,
        flight.arrivalAirport,
        brusselsDateTime(flight.std),
        brusselsDateTime(flight.sta),
    ];
    for (const cell of cells) {
        row.appendChild(createElementWithText("td", cell));
    }
    return row;
}

function render(flightsOfDay: FlightsOfDay): void {
    day = flightsOfDay.scheduledDepartureDate;
    dayField.value = day;
    const flights = flightsOfDay.flights;
    flightRows.replaceChildren(...flights.map(flightRow));
    flightsTable.classList.toggle("hidden", flights.length === 0);
    noFlights.classList.toggle("hidden", flights.length > 0);
    noFlights.textContent = `Geen vluchten op ${plainDate(day)}.`;
    overview.classList.remove("hidden");
}

/* Asks the list again; on failure what is on the screen stays and the reason is shown. */
async function reload(): Promise<boolean> {
    try {
        render(await getFlights(day));
        return true;
    } catch (error) {
        clearMessages(messageBox);
        addError(messageBox, (error as Error).message);
        return false;
    }
}

/* A new day goes into the address and the list is asked again. */
async function choose(): Promise<void> {
    clearMessages(messageBox);
    day = dayField.value || null;
    if (await reload() && day) {
        window.history.replaceState(null, "", overviewQuery(day));
    }
}

/* The entry of the application: after login it shows the role, the way to Load Planning and the flights of
   today. Both roles may look at everything; what a role may change, each service decides on every request. */
async function init(): Promise<void> {
    const role = await plannerRoleOrNoAccess(messageBox);
    if (role === null) {
        return;
    }
    document.getElementById("role")!.textContent = ROLE_NAME[role];
    flightsToLoadLink.href = loadPlanningAddress("html/flights-to-load.html");
    portal.classList.remove("hidden");
    dayField.addEventListener("change", choose);
    startAutoRefresh(reload, () => true);
}

init();
