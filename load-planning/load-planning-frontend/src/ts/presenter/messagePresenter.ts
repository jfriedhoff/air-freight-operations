import {createElementWithText} from "./htmlUtil.ts";

export function clearMessages(container: HTMLElement): void {
    container.classList.add("hidden");
    container.innerHTML = "";
    container.classList.remove("alert-error", "alert-info");
}

export function addInfo(container: HTMLElement, message: string): void {
    addMessage(container, message, "alert-info");
}

export function addError(container: HTMLElement, error: string): void {
    addMessage(container, error, "alert-error");
}

function addMessage(container: HTMLElement, message: string, level: string): void {
    container.classList.remove("hidden");
    container.classList.add(level);
    container.appendChild(createElementWithText("div", message));
}
