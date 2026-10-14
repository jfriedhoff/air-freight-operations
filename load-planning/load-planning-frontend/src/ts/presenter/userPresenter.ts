import {logout, userName} from "../infrastructure/auth.ts";

const userBox = document.getElementById("user-name")!;
const logoutButton = document.getElementById("logout-btn") as HTMLButtonElement;

async function init() {
    logoutButton.addEventListener("click", () => logout());
    try {
        userBox.textContent = await userName();
    } finally {
        // the body starts invisible, so the page does not flash before the redirect to Keycloak
        document.body.classList.remove("invisible");
    }
}

init();
