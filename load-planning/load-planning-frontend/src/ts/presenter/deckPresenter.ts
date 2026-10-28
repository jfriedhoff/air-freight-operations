import type {DeckCell, Pick, SideWeights} from "../domain/Deck.ts";
import {withoutLoadPosition} from "../domain/FlightCargo.ts";
import type {FlightCargo, Uld} from "../domain/FlightCargo.ts";
import {kilograms} from "../domain/Deck.ts";
import {createElementWithText} from "./htmlUtil.ts";

const deck = document.getElementById("deck")!;
const sideWeightsLine = document.getElementById("side-weights")!;
const offloadingLine = document.getElementById("offloading-reason")!;
const checksNote = document.getElementById("checks-note")!;
const traySection = document.getElementById("tray-section")!;
const tray = document.getElementById("tray")!;
const trayEmpty = document.getElementById("tray-empty")!;
const pickBar = document.getElementById("pick-bar")!;
const pickText = document.getElementById("pick-text")!;
const pickClearButton = document.getElementById("pick-clear-btn") as HTMLButtonElement;
const pickCancelButton = document.getElementById("pick-cancel-btn") as HTMLButtonElement;
let clearPicked: () => void = () => undefined;
let cancelPick: () => void = () => undefined;
pickClearButton.addEventListener("click", () => clearPicked());
pickCancelButton.addEventListener("click", () => cancelPick());

/* What a cell says to a screen reader: the same as the colours and symbols say to the eye. */
function labelOf(cell: DeckCell): string {
    const parts = [`Laadpositie ${cell.name}`];
    parts.push(cell.uld === null ? "vrij" : `${cell.uld}${cell.toArrival ? " naar de bestemming van de vlucht" : ""}`);
    if (cell.door) {
        parts.push("bij de vrachtdeur");
    }
    if (cell.stuck) {
        parts.push("kan niet uitgeladen worden");
    }
    if (cell.inTheWay) {
        parts.push("staat in de weg op een van de routes naar de deur");
    }
    if (cell.picked) {
        parts.push("gekozen");
    }
    return parts.join(", ");
}

function classesOf(cell: DeckCell): string {
    const classes = ["deck-cell"];
    if (cell.uld === null) {
        classes.push("deck-free");
    } else {
        classes.push(cell.toArrival ? "deck-to-arrival" : "deck-to-other");
    }
    if (cell.stuck) classes.push("deck-stuck");
    if (cell.inTheWay) classes.push("deck-in-the-way");
    if (cell.target) classes.push("deck-target");
    if (cell.picked) classes.push("deck-picked");
    if (cell.door) classes.push("deck-door");
    return classes.join(" ");
}

/* One button per load position on a grid of three rows: right side on top, the centre line, left side below. A
   position on the centre line (nose and tail) spans all three rows. A click is only passed on when the cell can be clicked; otherwise the button is there to be read. */
export function drawDeck(cells: DeckCell[], clickable: (cell: DeckCell) => boolean,
                         onClick: (cell: DeckCell) => void): void {
    const columns = Math.max(0, ...cells.map(cell => cell.column));
    deck.style.setProperty("--deck-columns", String(columns));
    const focused = focusedIn(deck, "position");
    deck.replaceChildren(...cells.map(cell => {
        const button = document.createElement("button");
        button.type = "button";
        button.dataset.position = cell.name;
        button.className = classesOf(cell);
        button.style.gridColumn = String(cell.column);
        button.style.gridRow = cell.row === "top" ? "1" : cell.row === "middle" ? "1 / 4" : "3";
        button.setAttribute("aria-label", labelOf(cell));
        button.appendChild(createElementWithText("span", cell.name)).className = "deck-name";
        if (cell.uld !== null) {
            button.appendChild(createElementWithText("span", cell.uld)).className = "deck-uld";
        }
        if (cell.stuck || cell.inTheWay) {
            // a symbol as well as a colour, so the marks are also clear without telling red apart
            const marks = (cell.stuck ? "✕" : "") + (cell.inTheWay ? "▲" : "");
            button.appendChild(createElementWithText("span", marks)).className = "deck-mark";
        }
        if (clickable(cell)) {
            button.setAttribute("aria-pressed", String(cell.picked));
            button.addEventListener("click", () => onClick(cell));
        } else {
            button.setAttribute("aria-disabled", "true");
        }
        return button;
    }));
    refocus(deck, "position", focused);
}

/* The deck and the tray are drawn anew on every answer: the keyboard stays on the same position or ULD. */
function focusedIn(container: HTMLElement, key: string): string | null {
    const active = document.activeElement;
    return active instanceof HTMLElement && container.contains(active) ? active.dataset[key] ?? null : null;
}

function refocus(container: HTMLElement, key: string, value: string | null): void {
    if (value !== null) {
        container.querySelector<HTMLElement>(`[data-${key}="${value}"]`)?.focus();
    }
}

/* Puts the keyboard on the first free position a picked ULD can go to. */
export function focusFirstTarget(): void {
    deck.querySelector<HTMLElement>(".deck-target")?.focus();
}

/* The findings of the checks next to the deck, or a note when they could not be asked. */
export function drawFindings(weights: SideWeights | null, offloadingReason: string | null,
                             note: string | null): void {
    sideWeightsLine.classList.toggle("hidden", weights === null);
    if (weights !== null) {
        const heavier = weights.passed ? null : weights.heavierSide;
        sideWeightsLine.replaceChildren(
            side("Links", weights.leftKilograms, heavier === "LEFT"),
            document.createTextNode(" · "),
            side("Rechts", weights.rightKilograms, heavier === "RIGHT"),
            document.createTextNode(" · de zwaarste kant mag hoogstens "
                + `${kilograms(weights.maximumOnTheHeaviestSide)} dragen`
                + (weights.passed ? "" : " (laterale balans niet in orde)")));
    }
    offloadingLine.classList.toggle("hidden", offloadingReason === null);
    offloadingLine.textContent = offloadingReason === null ? "" : `Offloading: ${offloadingReason}`;
    checksNote.classList.toggle("hidden", note === null);
    checksNote.textContent = note ?? "";
}

function side(name: string, weight: number, tooHeavy: boolean): HTMLElement {
    const element = createElementWithText("span", `${name} ${kilograms(weight)}`);
    if (tooHeavy) {
        element.className = "text-error font-semibold";
    }
    return element;
}

function weightOf(uld: Uld): string {
    return `${uld.weight.value} ${uld.weight.unit.toLowerCase()}`;
}

/* The booked ULDs without a load position, as buttons to pick one: only while the load planner can change the load. */
export function drawTray(cargo: FlightCargo, changeable: boolean, pick: Pick | null, onPick: (uld: Uld) => void): void {
    traySection.classList.toggle("hidden", !changeable);
    const waiting = withoutLoadPosition(cargo);
    trayEmpty.classList.toggle("hidden", waiting.length > 0);
    const focused = focusedIn(tray, "uld");
    tray.replaceChildren(...waiting.map(uld => {
        const picked = pick?.code === uld.code;
        const chip = createElementWithText("button", `${uld.code} · ${weightOf(uld)} · ${uld.destination}`);
        chip.className = `btn btn-sm font-mono ${picked ? "btn-warning" : "btn-outline"}`;
        chip.setAttribute("aria-pressed", String(picked));
        chip.dataset.uld = uld.code;
        chip.addEventListener("click", () => onPick(uld));
        return chip;
    }));
    refocus(tray, "uld", focused);
}

/* What the load planner is doing: which ULD is picked, and what a click does now. */
export function drawPick(pick: Pick | null, onClear: () => void, onCancel: () => void): void {
    pickBar.classList.toggle("hidden", pick === null);
    pickClearButton.classList.toggle("hidden", pick?.position == null);
    clearPicked = onClear;
    cancelPick = onCancel;
    if (pick !== null) {
        pickText.textContent = pick.position === null
            ? `Gekozen: ${pick.code}. Klik op een vrije laadpositie (groen) om de ULD te plaatsen.`
            : `Gekozen: ${pick.code} op ${pick.position}. Klik op een vrije laadpositie (groen) om te verplaatsen.`;
    }
}
