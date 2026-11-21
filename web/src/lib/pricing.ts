import type { Room } from '../api/schemas';

export type Estimate =
  | { kind: 'ok'; room: Room; total: number }
  | { kind: 'no-room'; maxCapacity: number };

/**
 * Mirrors the backend rule: the cheapest room that fits the party,
 * (price per person per hour x people + equipment prices) x hours.
 * Availability is only known to the server, which may pick a pricier free room or answer 409.
 */
export function estimate(rooms: Room[], people: number, hours: number, equipmentPrices: number[]): Estimate {
  const fitting = rooms
    .filter((r) => r.capacity >= people)
    .sort((a, b) => a.pricePerPersonPerHour - b.pricePerPersonPerHour || a.id - b.id);
  const room = fitting[0];
  if (!room) {
    return { kind: 'no-room', maxCapacity: rooms.reduce((max, r) => Math.max(max, r.capacity), 0) };
  }
  const equipmentPerHour = equipmentPrices.reduce((sum, p) => sum + p, 0);
  const total = Math.round((room.pricePerPersonPerHour * people + equipmentPerHour) * hours * 100) / 100;
  return { kind: 'ok', room, total };
}

/** Lowest hourly price per person across the rooms, or null without rooms. */
export function fromPrice(rooms: Room[]): number | null {
  if (rooms.length === 0) return null;
  return Math.min(...rooms.map((r) => r.pricePerPersonPerHour));
}
