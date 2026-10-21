import {flightOperationsAddress} from "../infrastructure/contexts.ts";

/* The top bar also links to pages of Flight Operations. Their address comes from the build, so the HTML only names
   the page. */
for (const link of document.querySelectorAll<HTMLAnchorElement>("a[data-flight-operations]")) {
    link.href = flightOperationsAddress(link.dataset.flightOperations!);
}
