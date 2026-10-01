export const SERVER_URL = "/api";

/* Throws an error for a response that is not ok, so presenters never look at a Response themselves. */
export function throwResponseError(message: string, response: Response) {
    throw new Error(`${message ? message + ":\n\t" : ""}${response.statusText} (${response.status}) on : ${response.url}`);
}
