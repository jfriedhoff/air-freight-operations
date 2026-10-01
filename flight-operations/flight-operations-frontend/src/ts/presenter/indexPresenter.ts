import {SERVER_URL} from "../infrastructure/restUtils.ts";

const status = document.getElementById("status");
if (status) {
    status.textContent = `Vluchten volgen. API: ${SERVER_URL}`;
}
