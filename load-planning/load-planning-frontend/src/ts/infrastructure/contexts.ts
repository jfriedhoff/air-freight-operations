/* Flight Operations is another deployment unit on another address, and its start page is the entry of the
   application. The address comes from the build (Vite), so another environment sets another value; nothing in
   the code names a host. Pages only link to it: a page of Load Planning never calls the API of Flight Operations. */
const FLIGHT_OPERATIONS_URL: string = import.meta.env.VITE_FLIGHT_OPERATIONS_URL ?? "http://localhost:8080";

/* A page of Flight Operations. The base may end with "/" or not, and may have a path of its own. */
export function flightOperationsAddress(page: string): string {
    return new URL(page, FLIGHT_OPERATIONS_URL.endsWith("/") ? FLIGHT_OPERATIONS_URL : FLIGHT_OPERATIONS_URL + "/").href;
}
