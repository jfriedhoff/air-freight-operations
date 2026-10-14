/* Load Planning is another deployment unit on another address. The address comes from the build (Vite), so
   another environment sets another value; nothing in the code names a host. Pages only link to it: a page of
   Flight Operations never calls the API of Load Planning. */
const LOAD_PLANNING_URL: string = import.meta.env.VITE_LOAD_PLANNING_URL ?? "http://localhost:8081";

/* A page of Load Planning. The base may end with "/" or not, and may have a path of its own. */
export function loadPlanningAddress(page: string): string {
    return new URL(page, LOAD_PLANNING_URL.endsWith("/") ? LOAD_PLANNING_URL : LOAD_PLANNING_URL + "/").href;
}
