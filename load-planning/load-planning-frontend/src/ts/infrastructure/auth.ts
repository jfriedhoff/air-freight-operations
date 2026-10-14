import Keycloak from "keycloak-js";

export type PlannerRole = "flight-planner" | "load-planner";

const PLANNER_ROLES: PlannerRole[] = ["flight-planner", "load-planner"];

/* Authorization code flow with PKCE: an unauthenticated visitor is sent to Keycloak and comes back with a code
   that keycloak-js exchanges for tokens. The tokens stay in memory, never in localStorage. */
const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8180",
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "air-freight",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "load-planning-frontend",
});

const loggedIn: Promise<boolean> = keycloak.init({
    onLoad: "login-required",
    pkceMethod: "S256",
    checkLoginIframe: false,
});

export async function userName(): Promise<string> {
    await loggedIn;
    return keycloak.tokenParsed?.preferred_username ?? "";
}

/* The planner role of the user, or null when the account has neither role or both: a user is either a flight
   planner or a load planner. Only decides what the page offers; the server checks the role on every request. */
export async function plannerRole(): Promise<PlannerRole | null> {
    await loggedIn;
    const roles = PLANNER_ROLES.filter(role => keycloak.hasRealmRole(role));
    return roles.length === 1 ? roles[0] : null;
}

export async function logout(): Promise<void> {
    await keycloak.logout({redirectUri: window.location.href});
}

/* fetch with the access token as bearer, refreshed first when it expires within 30 seconds */
export async function authorizedFetch(url: string, init: RequestInit = {}): Promise<Response> {
    await loggedIn;
    await keycloak.updateToken(30);
    const headers = new Headers(init.headers);
    headers.set("Authorization", `Bearer ${keycloak.token}`);
    return fetch(url, {...init, headers});
}
