import {LOAD_PLANNING_URL} from "../infrastructure/contexts.ts";
import {plannerRoleOrNoAccess} from "./accessPresenter.ts";

const messageBox = document.getElementById("message-box")!;
const portal = document.getElementById("portal")!;
const flightsToLoadLink = document.getElementById("flights-to-load-link") as HTMLAnchorElement;

const ROLE_NAME = {"flight-planner": "flight planner", "load-planner": "load planner"};

/* The entry of the application: after login it shows the role and the way into both contexts. Both roles may
   look at everything; what a role may change, each service decides on every request. */
async function init(): Promise<void> {
    const role = await plannerRoleOrNoAccess(messageBox);
    if (role === null) {
        return;
    }
    document.getElementById("role")!.textContent = ROLE_NAME[role];
    flightsToLoadLink.href = new URL("html/flights-to-load.html", LOAD_PLANNING_URL + "/").href;
    portal.classList.remove("hidden");
}

init();
