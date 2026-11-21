import { CalendarDays, CalendarX2, Clock, Compass, Users, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useCancelReservation, useMyReservations, useSpaces } from '../api/hooks';
import type { Reservation } from '../api/schemas';
import { ConfirmButton } from '../components/ConfirmButton';
import { Cover } from '../components/Cover';
import { StatusBadge } from '../components/StatusBadge';
import { EmptyState, ErrorState, Notice, Skeleton } from '../components/States';
import { formatDate, formatMoney, timeRange, todayIso } from '../lib/format';

function byDateTime(a: Reservation, b: Reservation): number {
  return `${a.date ?? ''}${a.time ?? ''}`.localeCompare(`${b.date ?? ''}${b.time ?? ''}`);
}

function BookingItem({ r, spaceName, today, cancelling, onCancel }: {
r: Reservation;
spaceName: string | null;
today: string;
cancelling: boolean;
onCancel: () => void;
}) {
  const spaceId = r.coworkingSpaceId ?? 0;
  return (
    <li className="list-item">
      <Cover spaceId={spaceId} name={spaceName} className="thumb" />
      <div>
        <div className="row" style={{ gap: 10 }}>
          <h3><Link to={`/spaces/${spaceId}`}>{spaceName ?? `Space #${spaceId}`}</Link></h3>
          <StatusBadge status={r.status} />
        </div>
        <div className="meta">
          <span><CalendarDays size={14} aria-hidden="true" />{formatDate(r.date)}</span>
          <span><Clock size={14} aria-hidden="true" />{timeRange(r)}</span>
          <span><Users size={14} aria-hidden="true" />{r.numberOfPeople}</span>
          <span className="price" style={{ color: 'var(--ink)' }}>{formatMoney(r.totalPrice)}</span>
        </div>
        {r.statusChangeReason && <p className="small muted" style={{ marginTop: 6 }}>“{r.statusChangeReason}”</p>}
      </div>
      <div className="list-actions">
        {(r.date ?? '') >= today && (
          <ConfirmButton
            label="Cancel"
            confirmLabel="Cancel booking"
            icon={<X size={14} aria-hidden="true" />}
            pending={cancelling}
            onConfirm={onCancel}
          />
        )}
      </div>
    </li>
  );
}

export function BookingsPage() {
  const reservations = useMyReservations(true);
  const spaces = useSpaces();
  const cancel = useCancelReservation();
  const names = new Map((spaces.data ?? []).map((s) => [s.id, s.name]));

  const today = todayIso();
  const all = reservations.data ?? [];
  const upcoming = all.filter((r) => (r.date ?? '') >= today).sort(byDateTime);
  const past = all.filter((r) => (r.date ?? '') < today).sort(byDateTime).reverse();

  return (
    <div className="container page">
      <div className="page-head">
        <div className="stack" style={{ gap: 6 }}>
          <p className="eyebrow">Your schedule</p>
          <h1 className="display">My bookings</h1>
        </div>
        <span className="spacer" />
        <Link to="/" className="btn"><Compass size={16} aria-hidden="true" />Find a space</Link>
      </div>

      {cancel.isError && <Notice tone="error">{errorMessage(cancel.error)}</Notice>}
      {reservations.isPending && <Skeleton height={240} />}
      {reservations.isError && <ErrorState message={errorMessage(reservations.error)} onRetry={() => reservations.refetch()} />}
      {reservations.isSuccess && all.length === 0 && (
        <div className="card">
          <EmptyState icon={<CalendarX2 size={24} />} title="No bookings yet" action={<Link className="btn btn-primary" to="/">Explore spaces</Link>}>
            When you request a room it shows up here, with its status.
          </EmptyState>
        </div>
      )}
      {all.length > 0 && (
        <div className="stack-lg">
          <section className="stack" aria-labelledby="upcoming-title">
            <h2 id="upcoming-title" className="section-title">Upcoming <span className="muted">· {upcoming.length}</span></h2>
            {upcoming.length === 0 ? (
              <p className="muted">Nothing coming up.</p>
            ) : (
              <ul className="card list" style={{ padding: 0, margin: 0, listStyle: 'none' }}>
                {upcoming.map((r) => (
                  <BookingItem key={r.id} r={r} spaceName={names.get(r.coworkingSpaceId ?? 0) ?? null} today={today}
                    cancelling={cancel.isPending && cancel.variables === r.id} onCancel={() => cancel.mutate(r.id)} />
                ))}
              </ul>
            )}
          </section>
          {past.length > 0 && (
            <section className="stack" aria-labelledby="past-title">
              <h2 id="past-title" className="section-title">Past</h2>
              <ul className="card list" style={{ padding: 0, margin: 0, listStyle: 'none' }}>
                {past.map((r) => (
                  <BookingItem key={r.id} r={r} spaceName={names.get(r.coworkingSpaceId ?? 0) ?? null} today={today}
                    cancelling={cancel.isPending && cancel.variables === r.id} onCancel={() => cancel.mutate(r.id)} />
                ))}
              </ul>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
