import { ArrowLeft, CheckCircle2, MapPin, Users } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, errorMessage } from '../api/client';
import { useBook, useEquipment, useRooms, useSpace } from '../api/hooks';
import type { Reservation } from '../api/schemas';
import { useCurrentUser } from '../auth/AuthContext';
import { BookingForm } from '../components/BookingForm';
import { Cover } from '../components/Cover';
import { hasLocation, SpaceMap } from '../components/SpaceMap';
import { EmptyState, ErrorState, Notice, Skeleton } from '../components/States';
import { EQUIPMENT_LABELS, formatDate, formatMoney, timeRange } from '../lib/format';

export function SpacePage() {
  const id = Number(useParams().id);
  const [params] = useSearchParams();
  const location = useLocation();
  const user = useCurrentUser();
  const space = useSpace(id);
  const rooms = useRooms(id);
  const equipment = useEquipment(id);
  const book = useBook();
  const [confirmed, setConfirmed] = useState<Reservation | null>(null);

  if (!Number.isInteger(id)) return <ErrorState message="This link doesn't point to a space." />;

  if (space.isPending) {
    return (
      <div className="container page stack-lg" aria-busy="true">
        <Skeleton height={260} />
        <Skeleton height={40} style={{ maxWidth: 420 }} />
      </div>
    );
  }
  if (space.isError) {
    const notFound = space.error instanceof ApiError && space.error.status === 404;
    return notFound ? (
      <EmptyState icon={<MapPin size={24} />} title="Space not found" action={<Link className="btn" to="/">Back to explore</Link>}>
        It may have been removed by its owner.
      </EmptyState>
    ) : (
      <ErrorState message={errorMessage(space.error)} onRetry={() => space.refetch()} />
    );
  }

  const s = space.data;
  const roomList = [...(rooms.data ?? [])].sort((a, b) => a.capacity - b.capacity);
  const bookError = book.error ? errorMessage(book.error) : null;
  const conflict = book.error instanceof ApiError && book.error.status === 409;

  return (
    <div className="container page">
      <Link to="/" className="btn btn-ghost btn-sm" style={{ marginBottom: 'var(--space-4)' }}>
        <ArrowLeft size={15} aria-hidden="true" /> All spaces
      </Link>
      <Cover spaceId={s.id} name={s.name} className="cover-hero" />

      <div className="space-layout" style={{ marginTop: 'var(--space-6)' }}>
        <div className="stack-lg">
          <header className="stack" style={{ gap: 'var(--space-2)' }}>
            <h1 className="display space-title">{s.name ?? 'Unnamed space'}</h1>
            <p className="row muted"><MapPin size={16} aria-hidden="true" />{s.address || 'Address not listed'}</p>
          </header>

          <section aria-labelledby="rooms-title" className="stack">
            <h2 id="rooms-title" className="section-title">Rooms</h2>
            {rooms.isPending && <Skeleton height={90} />}
            {rooms.isError && <Notice tone="error">{errorMessage(rooms.error)}</Notice>}
            {rooms.isSuccess && roomList.length === 0 && <p className="muted">The owner hasn't added rooms yet.</p>}
            {roomList.length > 0 && (
              <ul className="room-list" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {roomList.map((r) => (
                  <li key={r.id} className="room">
                    <span className="cap"><Users size={16} aria-hidden="true" />{r.capacity === 1 ? 'Solo desk' : `Up to ${r.capacity}`}</span>
                    <span className="price">{formatMoney(r.pricePerPersonPerHour)}</span>
                    <span className="muted small">per person per hour</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section aria-labelledby="equipment-title" className="stack">
            <h2 id="equipment-title" className="section-title">Equipment on request</h2>
            {equipment.isSuccess && equipment.data.length === 0 && <p className="muted">No bookable equipment listed.</p>}
            {equipment.data && equipment.data.length > 0 && (
              <div className="chips">
                {equipment.data.map((e) => (
                  <span key={e.id} className="chip" style={{ cursor: 'default' }}>
                    {e.typeOfEquipment ? EQUIPMENT_LABELS[e.typeOfEquipment] : e.name}
                    <span className="chip-price">{formatMoney(e.price)}/h</span>
                  </span>
                ))}
              </div>
            )}
          </section>

          {hasLocation(s) && (
            <section aria-labelledby="where-title" className="stack">
              <h2 id="where-title" className="section-title">Where you'll be</h2>
              <SpaceMap className="mini-map" spaces={[s]} labels={{ [s.id]: s.name ?? 'Here' }} activeId={s.id} label={`Location of ${s.name ?? 'this space'}`} />
            </section>
          )}
        </div>

        <aside className="booking-panel card card-pad stack" aria-labelledby="book-title">
          <h2 id="book-title" className="section-title">Book a room</h2>
          {confirmed ? (
            <div className="stack">
              <Notice tone="success">
                <strong>Request sent.</strong> {formatDate(confirmed.date)}, {timeRange(confirmed)} for{' '}
                {confirmed.numberOfPeople} — {formatMoney(confirmed.totalPrice)}. The owner will confirm it.
              </Notice>
              <Link to="/bookings" className="btn btn-primary btn-block"><CheckCircle2 size={16} aria-hidden="true" />View my bookings</Link>
              <button type="button" className="btn btn-block" onClick={() => { setConfirmed(null); book.reset(); }}>Book another slot</button>
            </div>
          ) : (
            <>
              {bookError && (
                <Notice tone="error">
                  {conflict ? (
                    <><strong>That time is taken.</strong> Every room that fits your group is already booked then. Try another time or day.</>
                  ) : (
                    bookError
                  )}
                </Notice>
              )}
              <BookingForm
                rooms={roomList}
                equipment={equipment.data ?? []}
                initialPeople={Number(params.get('people') ?? '1') || 1}
                pending={book.isPending}
                onSubmit={(values) =>
                  book.mutate({ ...values, coworkingSpaceId: s.id }, { onSuccess: (created) => setConfirmed(created) })
                }
                submitSlot={
                  user ? undefined : (
                    <Link to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} className="btn btn-primary btn-lg btn-block">
                      Log in to book
                    </Link>
                  )
                }
              />
            </>
          )}
        </aside>
      </div>
    </div>
  );
}
