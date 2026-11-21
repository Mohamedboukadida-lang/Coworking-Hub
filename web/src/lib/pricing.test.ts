import { estimate, fromPrice } from './pricing';

const rooms = [
  { id: 1, capacity: 1, pricePerPersonPerHour: 2 },
  { id: 2, capacity: 6, pricePerPersonPerHour: 12 },
  { id: 3, capacity: 4, pricePerPersonPerHour: 10 },
];

describe('estimate', () => {
  it('uses the cheapest room that fits, like the backend', () => {
    // (10 x 2 people + 5.5 equipment) x 3 hours
    expect(estimate(rooms, 2, 3, [5.5])).toEqual({ kind: 'ok', room: rooms[2], total: 76.5 });
  });

  it('moves to a bigger room when the party grows', () => {
    const result = estimate(rooms, 5, 1, []);
    expect(result.kind === 'ok' && result.room.id).toBe(2);
  });

  it('rounds to cents', () => {
    const result = estimate([{ id: 1, capacity: 2, pricePerPersonPerHour: 3.333 }], 1, 1, []);
    expect(result.kind === 'ok' && result.total).toBe(3.33);
  });

  it('reports the largest room when nothing fits', () => {
    expect(estimate(rooms, 10, 1, [])).toEqual({ kind: 'no-room', maxCapacity: 6 });
    expect(estimate([], 1, 1, [])).toEqual({ kind: 'no-room', maxCapacity: 0 });
  });
});

describe('fromPrice', () => {
  it('is the lowest per-person price, or null without rooms', () => {
    expect(fromPrice(rooms)).toBe(2);
    expect(fromPrice([])).toBeNull();
  });
});
