import {FLIGHT_OPERATIONS_URL} from "../infrastructure/contexts.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";

const messageBox = document.getElementById("message-box")!;
const start = document.getElementById("start")!;
const portalLink = document.getElementById("portal-link") as HTMLAnchorElement;

/* The start page of Load Planning: the way back to the entry of the application, and, for a user with exactly
   one planner role, the way into Load Planning. */
async function init(): Promise<void> {
    portalLink.href = new URL("/", FLIGHT_OPERATIONS_URL).href;
    if (await plannerRoleOrNoAccess(messageBox) === null) {
        return;
    }
    start.classList.remove("hidden");
}

init();
