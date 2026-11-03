import {overviewQuery} from "../domain/FlightOverview.ts";
import type {FlightRow, FlightsOfDay} from "../domain/FlightOverview.ts";
import {getFlights} from "../infrastructure/flightRest.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";
import {startAutoRefresh} from "./autoRefresh.ts";
import {
    addDays,
    brusselsDate,
    brusselsDayMonth,
    brusselsHour,
    brusselsShortDateTime,
    brusselsTimeWithZone,
    longDate,
    plainDate,
} from "./brusselsTime.ts";
import {barPlace, brusselsMidnightPercent, hoursOf, stillInTheAir, timelineOf} from "./ganttTimeline.ts";
import type {Timeline} from "./ganttTimeline.ts";
import {createElementWithText} from "./htmlUtil.ts";
import {addError, clearMessages} from "./messagePresenter.ts";

const messageBox = document.getElementById("message-box")!;
const gantt = document.getElementById("gantt")!;
const backLink = document.getElementById("back-to-overview") as HTMLAnchorElement;
const dayField = document.getElementById("day") as HTMLInputElement;
const previousDayButton = document.getElementById("previous-day") as HTMLButtonElement;
const nextDayButton = document.getElementById("next-day") as HTMLButtonElement;
const dayTitle = document.getElementById("day-title")!;
const flightCount = document.getElementById("flight-count")!;
const chart = document.getElementById("chart")!;
const hours = document.getElementById("hours")!;
const rows = document.getElementById("rows")!;
const noFlights = document.getElementById("no-flights")!;

const COLUMNS = "grid grid-cols-[6.5rem_6rem_8.5rem_8.5rem_1fr]";

/* Same as the overview: the address holds the chosen day; without one the server answers with today (UTC). */
let day: string | null = new URLSearchParams(window.location.search).get("scheduledDepartureDate");

/* A time on the shown day needs no date; a time on another day in Brussels does. */
function momentText(instant: string, shownDay: string): string {
    return brusselsDate(instant) === shownDay ? brusselsTimeWithZone(instant) : brusselsShortDateTime(instant);
}

/* The hours in Brussels time; at Brussels midnight the label is the new date. */
function hourAxis(timeline: Timeline): HTMLElement[] {
    return hoursOf(timeline).map(hour => {
        const hourText = brusselsHour(hour.moment);
        const midnight = hourText === "00";
        const tick = createElementWithText("span", midnight ? brusselsDayMonth(hour.moment) : hourText);
        tick.className = midnight
            ? "absolute inset-y-0 border-l border-dashed border-base-content/50 pl-1 py-2 font-semibold"
            : "absolute inset-y-0 border-l border-base-300 pl-1 py-2";
        tick.style.left = `${hour.percent}%`;
        return tick;
    });
}

function cell(text: string, className: string): HTMLElement {
    const element = createElementWithText("span", text);
    element.className = className;
    return element;
}

function flightRow(flight: FlightRow, timeline: Timeline, shownDay: string, midnightPercent: number): HTMLElement {
    const details = `flight-details.html?flightId=${encodeURIComponent(flight.id)}`;
    const std = momentText(flight.std, shownDay);
    const sta = momentText(flight.sta, shownDay);
    const row = document.createElement("div");
    row.className = `${COLUMNS} items-center border-b border-base-200 last:border-b-0 hover:bg-base-200/50`;

    const place = barPlace(timeline, flight.std, flight.sta);
    const link = createElementWithText("a", flight.flightNumber) as HTMLAnchorElement;
    link.href = details;
    link.className = "link link-hover font-medium";
    const flightCell = document.createElement("div");
    flightCell.className = "px-4 py-1";
    flightCell.append(link);
    if (place.cutOffAtStart) {
        // In Brussels time a flight of the day before can depart on the shown day, so its own day is named.
        flightCell.append(cell(plainDate(flight.scheduledDepartureDate), "block text-xs opacity-60"));
    }

    const track = document.createElement("div");
    track.className = "relative h-9 hour-grid";

    const midnight = document.createElement("div");
    midnight.className = "absolute inset-y-0 border-l border-dashed border-base-content/50";
    midnight.style.left = `${midnightPercent}%`;

    // The bar opens Vluchtdetails like the flight number. It is left out of the tab order because the
    // flight number already is in it. A bar cut off at the end of the day has a square right end. A flight
    // of the day before is lighter and has a square left end: it belongs to that day.
    const bar = document.createElement("a");
    bar.href = details;
    bar.tabIndex = -1;
    bar.title = `${flight.flightNumber} ${flight.departureAirport}–${flight.arrivalAirport}: ${std} – ${sta}`;
    bar.className = "absolute top-2.5 bottom-2.5 rounded-sm hover:brightness-110"
        + (place.cutOffAtStart ? " bg-primary/50 rounded-l-none" : " bg-primary")
        + (place.cutOffAtEnd ? " rounded-r-none" : "");
    bar.style.left = `${place.leftPercent}%`;
    bar.style.width = `${place.widthPercent}%`;

    track.append(midnight, bar);
    row.append(flightCell, cell(`${flight.departureAirport}–${flight.arrivalAirport}`, "opacity-70"),
        cell(std, ""), cell(sta, ""), track);
    return row;
}

/* Only the flights of the day are counted: a flight of the day before keeps its own day. */
function countText(ofDay: number, fromDayBefore: number): string {
    const count = ofDay === 1 ? "1 vlucht" : `${ofDay} vluchten`;
    return fromDayBefore === 0 ? count : `${count}, en ${fromDayBefore} van de vorige dag nog onderweg`;
}

function render(flightsOfDay: FlightsOfDay, flightsOfDayBefore: FlightRow[]): void {
    const shownDay = flightsOfDay.scheduledDepartureDate;
    const flights = flightsOfDay.flights;
    day = shownDay;
    dayField.value = shownDay;
    backLink.href = `../index.html${overviewQuery(shownDay)}`;
    dayTitle.textContent = `Vluchten van ${longDate(shownDay)}`;

    const timeline = timelineOf(shownDay);
    const carriedOver = stillInTheAir(timeline, flightsOfDayBefore);
    const shown = [...carriedOver, ...flights];
    flightCount.textContent = countText(flights.length, carriedOver.length);
    const midnightPercent = brusselsMidnightPercent(timeline, shownDay);
    hours.replaceChildren(...hourAxis(timeline));
    rows.replaceChildren(...shown.map(flight => flightRow(flight, timeline, shownDay, midnightPercent)));
    chart.classList.toggle("hidden", shown.length === 0);
    noFlights.classList.toggle("hidden", shown.length > 0);
    noFlights.textContent = `Geen vluchten op ${plainDate(shownDay)}.`;
}

/* Asks the flights again; on failure what is on the screen stays and the reason is shown. The day itself comes
   first: without a day in the address the server chooses it, and the day before follows from it. As on the
   overview, an answer for a day the user has left in the meantime is dropped, so a late automatic reload cannot
   put the old day back. */
async function reload(): Promise<boolean> {
    const askedDay = day;
    try {
        const flightsOfDay = await getFlights(askedDay);
        const dayBefore = await getFlights(addDays(flightsOfDay.scheduledDepartureDate, -1));
        if (day !== askedDay) {
            return false;
        }
        render(flightsOfDay, dayBefore.flights);
        return true;
    } catch (error) {
        if (day === askedDay) {
            clearMessages(messageBox);
            addError(messageBox, (error as Error).message);
        }
        return false;
    }
}

/* A new day goes into the address and the flights are asked again. */
async function choose(): Promise<void> {
    clearMessages(messageBox);
    day = dayField.value || null;
    if (await reload() && day) {
        window.history.replaceState(null, "", overviewQuery(day));
    }
}

function moveDays(days: number): void {
    if (day) {
        dayField.value = addDays(day, days);
        choose();
    }
}

/* Both roles may look at the chart; it changes nothing. */
async function init(): Promise<void> {
    if (await plannerRoleOrNoAccess(messageBox) === null) {
        return;
    }
    gantt.classList.remove("hidden");
    dayField.addEventListener("change", choose);
    previousDayButton.addEventListener("click", () => moveDays(-1));
    nextDayButton.addEventListener("click", () => moveDays(1));
    startAutoRefresh(reload, () => true);
}

init();
