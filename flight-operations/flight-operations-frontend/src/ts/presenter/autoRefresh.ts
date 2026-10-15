import {brusselsTime} from "./brusselsTime.ts";

const REFRESH_EVERY_MS = 45_000;

const refreshButton = document.getElementById("refresh-btn") as HTMLButtonElement;
const updatedAt = document.getElementById("updated-at")!;

/* Keeps a page up to date: it loads now, every 45 seconds, when the tab becomes visible again and when the user
   asks for it. The page only asks the server again; no rule is decided here. `reload` answers whether it
   succeeded; `mayReload` lets a page skip an automatic reload, for instance while a choice is not sent yet.
   The button always reloads. */
export function startAutoRefresh(reload: () => Promise<boolean>, mayReload: () => boolean): void {
    async function refresh(asked: boolean): Promise<void> {
        if (!asked && !mayReload()) {
            return;
        }
        if (await reload()) {
            updatedAt.textContent = `Bijgewerkt om ${brusselsTime(new Date())}`;
        }
    }

    refreshButton.addEventListener("click", () => refresh(true));
    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") {
            refresh(false);
        }
    });
    // A tab nobody looks at asks nothing: every request also renews the token, and a forgotten tab would keep
    // the session alive for ever. When the tab is shown again, the handler above reloads at once.
    window.setInterval(() => {
        if (document.visibilityState === "visible") {
            refresh(false);
        }
    }, REFRESH_EVERY_MS);
    refresh(true);
}
