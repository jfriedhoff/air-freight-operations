import {plannerRoleOrNoAccess} from "./accessPresenter.ts";

const messageBox = document.getElementById("message-box")!;
const start = document.getElementById("start")!;

/* The start page of Load Planning: for a user with exactly one planner role, the way into Load Planning. The way
   back to the entry of the application is in the top bar. */
async function init(): Promise<void> {
    if (await plannerRoleOrNoAccess(messageBox) === null) {
        return;
    }
    start.classList.remove("hidden");
}

init();
