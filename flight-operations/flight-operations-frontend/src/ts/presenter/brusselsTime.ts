/* The service stores and sends UTC; the Brussels staff read Brussels time. Every time is shown with its date and
   with the short name of the zone: on the night the clock goes back, 02:30 exists twice (CEST and CET). */
const BRUSSELS = new Intl.DateTimeFormat("nl-BE", {
    timeZone: "Europe/Brussels",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZoneName: "short",
});

function parts(moment: Date): Record<string, string> {
    return Object.fromEntries(BRUSSELS.formatToParts(moment).map(part => [part.type, part.value]));
}

/* "09/01/2026 07:30 CET" */
export function brusselsDateTime(instant: string): string {
    const p = parts(new Date(instant));
    return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute} ${p.timeZoneName}`;
}

/* "07:31", for a moment of today such as the last update of the page */
export function brusselsTime(moment: Date): string {
    const p = parts(moment);
    return `${p.hour}:${p.minute}`;
}

/* "07:30 CET", for a moment on the day that is shown */
export function brusselsTimeWithZone(instant: string): string {
    const p = parts(new Date(instant));
    return `${p.hour}:${p.minute} ${p.timeZoneName}`;
}

/* "10/01 01:30 CET", for a moment on another day than the one that is shown */
export function brusselsShortDateTime(instant: string): string {
    const p = parts(new Date(instant));
    return `${p.day}/${p.month} ${p.hour}:${p.minute} ${p.timeZoneName}`;
}

/* "07", the hour of a moment in Brussels */
export function brusselsHour(moment: Date): string {
    return parts(moment).hour;
}

/* "10/01", the date of a moment in Brussels without the year */
export function brusselsDayMonth(moment: Date): string {
    const p = parts(moment);
    return `${p.day}/${p.month}`;
}

/* The date of a moment in Brussels, as "2026-01-10" */
export function brusselsDate(instant: string): string {
    const p = parts(new Date(instant));
    return `${p.year}-${p.month}-${p.day}`;
}

/* 00:00 Brussels time on this date, as a moment. The offset is read at 00:00 UTC of the same date: Brussels
   changes its clock at 01:00 UTC, so the offset at Brussels midnight is the same. */
export function brusselsMidnight(isoDate: string): Date {
    const [year, month, day] = isoDate.split("-").map(Number);
    const utcMidnight = Date.UTC(year, month - 1, day);
    const p = parts(new Date(utcMidnight));
    const brusselsAsUtc = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute);
    return new Date(utcMidnight - (brusselsAsUtc - utcMidnight));
}

/* The date a number of days later (or earlier), as "2026-01-10" */
export function addDays(isoDate: string, days: number): string {
    const [year, month, day] = isoDate.split("-").map(Number);
    return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

/* "vrijdag 9 januari 2026", for a date without a time such as the scheduled departure date */
export function longDate(isoDate: string): string {
    return new Intl.DateTimeFormat("nl-BE", {
        timeZone: "UTC",
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    }).format(new Date(`${isoDate}T00:00:00Z`));
}

/* A date without a time, such as the scheduled departure date: shown as it is, it has no time zone to convert. */
export function plainDate(isoDate: string): string {
    const [year, month, day] = isoDate.split("-");
    return `${day}/${month}/${year}`;
}
