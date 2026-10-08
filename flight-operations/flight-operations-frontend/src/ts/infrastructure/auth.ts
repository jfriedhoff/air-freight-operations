import Keycloak from "keycloak-js";

export const FLIGHT_PLANNER = "flight-planner";

/* Authorization code flow with PKCE: an unauthenticated visitor is sent to Keycloak and comes back with a code
   that keycloak-js exchanges for tokens. The tokens stay in memory, never in localStorage. */
const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8180",
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "air-freight",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "flight-operations-frontend",
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

/* Only decides what the page offers. The server checks the role again on every request. */
export async function hasRole(role: string): Promise<boolean> {
    await loggedIn;
    return keycloak.hasRealmRole(role);
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
