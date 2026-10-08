export const SERVER_URL = "/api";

interface Problem {
    detail?: string;
}

/* Throws an error for a response that is not ok, with the reason the server gives (Problem Details),
   so presenters never look at a Response themselves. */
export async function throwResponseError(message: string, response: Response): Promise<never> {
    const problem: Problem | null = await response.json().catch(() => null);
    const reason = problem?.detail ?? reasonFor(response.status);
    throw new Error(`${message}: ${reason}`);
}

/* 401 and 403 come from the security filter and carry no reason of their own. */
function reasonFor(status: number): string {
    switch (status) {
        case 401:
            return "u bent niet aangemeld";
        case 403:
            return "u hebt geen rechten voor deze actie";
        default:
            return `onverwacht antwoord van de server (${status})`;
    }
}
