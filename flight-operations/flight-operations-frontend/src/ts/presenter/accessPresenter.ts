import {plannerRole} from "../infrastructure/auth.ts";
import type {PlannerRole} from "../infrastructure/auth.ts";
import {addError} from "./messagePresenter.ts";

const NO_ACCESS = "U hebt geen toegang: uw account moet precies één van de rollen flight planner of load planner"
    + " hebben.";

/* The role of the user, or null after telling the user why there is no access. The page then asks the server
   nothing: it would answer 403 without a reason. The header with the user name and "Afmelden" stays. */
export async function plannerRoleOrNoAccess(messageBox: HTMLElement): Promise<PlannerRole | null> {
    const role = await plannerRole();
    if (role === null) {
        addError(messageBox, NO_ACCESS);
    }
    return role;
}
