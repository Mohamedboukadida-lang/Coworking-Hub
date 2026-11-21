import type { ReservationStatus } from '../api/schemas';

const LABELS: Record<ReservationStatus, string> = {
  PENDING: 'Pending',
  ACCEPTED: 'Confirmed',
  DECLINED: 'Declined',
};

export function StatusBadge({ status }: { status: ReservationStatus | null }) {
  const s = status ?? 'PENDING';
  return <span className={`badge badge-${s.toLowerCase()}`}>{LABELS[s]}</span>;
}
