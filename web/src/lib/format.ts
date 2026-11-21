import type { EquipmentType, Reservation } from '../api/schemas';

const money = new Intl.NumberFormat('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function formatMoney(amount: number): string {
  return `${money.format(amount)} TND`;
}

export function formatDate(iso: string | null): string {
  if (!iso) return 'No date';
  const [y, m, d] = iso.split('-').map(Number);
  if (!y || !m || !d) return iso;
  return new Intl.DateTimeFormat('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(Date.UTC(y, m - 1, d)));
}

export function timeRange(r: Pick<Reservation, 'time' | 'hours'>): string {
  if (!r.time || !r.hours) return r.time ?? '';
  const [h = 0, min = 0] = r.time.split(':').map(Number);
  const end = h * 60 + min + r.hours * 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${r.time}–${pad(Math.floor(end / 60))}:${pad(end % 60)}`;
}

/** Latest start hour so that a booking of {@code hours} still ends by midnight. */
export function latestStart(hours: number): string {
  return `${String(24 - hours).padStart(2, '0')}:00`;
}

export const EQUIPMENT_LABELS: Record<EquipmentType, string> = {
  COMPUTER: 'Computer',
  PROJECTOR: 'Projector',
  WHITEBOARD: 'Whiteboard',
  CHAIR: 'Ergonomic chair',
  TABLE: 'Extra table',
  COFFEE_MACHINE: 'Coffee machine',
};

/** A stable hue per space, used for its generated cover art (spaces have no photos yet). */
export function spaceHue(id: number): number {
  return (id * 47 + 160) % 360;
}

export function todayIso(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}
