import {SERVER_URL} from "../infrastructure/restUtils.ts";

const status = document.getElementById("status");
if (status) {
    status.textContent = `Lading plannen. API: ${SERVER_URL}`;
}
