import {loadPlanningAddress} from "../infrastructure/contexts.ts";

/* The top bar also links to pages of Load Planning. Their address comes from the build, so the HTML only names
   the page. */
for (const link of document.querySelectorAll<HTMLAnchorElement>("a[data-load-planning]")) {
    link.href = loadPlanningAddress(link.dataset.loadPlanning!);
}
