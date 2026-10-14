/* Load Planning is another deployment unit on another address. The address comes from the build (Vite), so
   another environment sets another value; nothing in the code names a host. Pages only link to it: a page of
   Flight Operations never calls the API of Load Planning. */
export const LOAD_PLANNING_URL: string = import.meta.env.VITE_LOAD_PLANNING_URL ?? "http://localhost:8081";
