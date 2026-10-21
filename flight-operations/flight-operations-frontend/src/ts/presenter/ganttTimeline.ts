import {addDays, brusselsMidnight} from "./brusselsTime.ts";

const HOUR_MS = 3_600_000;
/* A bar narrower than this would be invisible, for instance a flight that departs at 23:59 UTC. */
const MIN_WIDTH_PERCENT = 0.4;

/* The timeline of a chosen day is that UTC day, from 00:00 to 24:00 UTC, because the day of a flight is its
   scheduled departure date in UTC. So every departure of the day has its place on it, also one at 23:59 UTC
   (00:59 the next day in Brussels). The page labels the hours in Brussels time. */
export interface Timeline {
    start: number;
    end: number;
}

export interface BarPlace {
    leftPercent: number;
    widthPercent: number;
    cutOff: boolean;
}

export function timelineOf(day: string): Timeline {
    const start = Date.parse(`${day}T00:00:00Z`);
    return {start, end: start + 24 * HOUR_MS};
}

/* Where a moment is on the timeline, in percent; a moment outside it is put on the edge. */
function percentOf(timeline: Timeline, moment: number): number {
    const clamped = Math.min(Math.max(moment, timeline.start), timeline.end);
    return (clamped - timeline.start) / (timeline.end - timeline.start) * 100;
}

/* The bar of a flight from STD to STA. An arrival after the end of the day is cut off at the edge. */
export function barPlace(timeline: Timeline, std: string, sta: string): BarPlace {
    const left = percentOf(timeline, Date.parse(std));
    const width = Math.max(percentOf(timeline, Date.parse(sta)) - left, MIN_WIDTH_PERCENT);
    return {
        leftPercent: Math.min(left, 100 - width),
        widthPercent: width,
        cutOff: Date.parse(sta) > timeline.end,
    };
}

/* The start of every hour on the timeline, with its place in percent. */
export function hoursOf(timeline: Timeline): { moment: Date, percent: number }[] {
    const hours = [];
    for (let moment = timeline.start; moment < timeline.end; moment += HOUR_MS) {
        hours.push({moment: new Date(moment), percent: percentOf(timeline, moment)});
    }
    return hours;
}

/* Where the next day starts in Brussels: the UTC day ends one or two hours later. */
export function brusselsMidnightPercent(timeline: Timeline, day: string): number {
    return percentOf(timeline, brusselsMidnight(addDays(day, 1)).getTime());
}
