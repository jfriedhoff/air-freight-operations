/* The service stores and sends UTC; the Brussels staff read Brussels time. Every time is shown with its date. */
const BRUSSELS = new Intl.DateTimeFormat("nl-BE", {
    timeZone: "Europe/Brussels",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
});

export function brusselsDateTime(instant: string): string {
    return BRUSSELS.format(new Date(instant));
}

/* A date without a time, such as the scheduled departure date: shown as it is, it has no time zone to convert. */
export function plainDate(isoDate: string): string {
    const [year, month, day] = isoDate.split("-");
    return `${day}/${month}/${year}`;
}
