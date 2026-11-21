import { Building2, Inbox, Plus, Wallet } from 'lucide-react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useSpaceReservations, useSpaces } from '../api/hooks';
import type { Reservation } from '../api/schemas';
import { useCurrentUser } from '../auth/AuthContext';
import { Cover } from '../components/Cover';
import { RequestItem } from '../components/RequestItem';
import { EmptyState, ErrorState, Skeleton } from '../components/States';
import { formatMoney } from '../lib/format';

export function ManagePage() {
  const user = useCurrentUser();
  const spaces = useSpaces();
  const isAdmin = user?.typeCompte === 'ADMIN';
  const mine = (spaces.data ?? []).filter((s) => isAdmin || s.ownerId === user?.id);
  const reservationQueries = useSpaceReservations(mine.map((s) => s.id));
  const names = new Map(mine.map((s) => [s.id, s.name]));

  const all: Reservation[] = reservationQueries.flatMap((q) => q.data ?? []);
  const pending = all
    .filter((r) => r.status === 'PENDING')
    .sort((a, b) => `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`));
  const revenue = all.filter((r) => r.status === 'ACCEPTED').reduce((sum, r) => sum + r.totalPrice, 0);
  const pendingBySpace = new Map<number, number>();
  for (const r of pending) {
    const id = r.coworkingSpaceId ?? 0;
    pendingBySpace.set(id, (pendingBySpace.get(id) ?? 0) + 1);
  }

  return (
    <div className="container page">
      <div className="page-head">
        <div className="stack" style={{ gap: 6 }}>
          <p className="eyebrow">{isAdmin ? 'Administrator' : 'Owner dashboard'}</p>
          <h1 className="display">Manage your spaces</h1>
        </div>
        <span className="spacer" />
        <Link to="/manage/new" className="btn btn-primary"><Plus size={16} aria-hidden="true" />New space</Link>
      </div>

      {spaces.isPending && <Skeleton height={300} />}
      {spaces.isError && <ErrorState message={errorMessage(spaces.error)} onRetry={() => spaces.refetch()} />}

      {spaces.isSuccess && mine.length === 0 && (
        <div className="card">
          <EmptyState icon={<Building2 size={24} />} title="List your first space"
            action={<Link to="/manage/new" className="btn btn-primary"><Plus size={16} aria-hidden="true" />New space</Link>}>
            Add your space, its rooms and prices, and start receiving booking requests.
          </EmptyState>
        </div>
      )}

      {mine.length > 0 && (
        <div className="stack-lg">
          <div className="stats">
            <div className="card stat"><div className="label"><Building2 size={16} aria-hidden="true" />Spaces</div><div className="value">{mine.length}</div></div>
            <div className="card stat"><div className="label"><Inbox size={16} aria-hidden="true" />Requests to review</div><div className="value">{pending.length}</div></div>
            <div className="card stat"><div className="label"><Wallet size={16} aria-hidden="true" />Confirmed bookings</div><div className="value">{formatMoney(revenue)}</div></div>
          </div>

          <section className="stack" aria-labelledby="inbox-title">
            <h2 id="inbox-title" className="section-title">Requests to review</h2>
            {pending.length === 0 ? (
              <div className="card"><EmptyState icon={<Inbox size={24} />} title="You're all caught up">New booking requests appear here.</EmptyState></div>
            ) : (
              <ul className="card list" style={{ padding: 0, margin: 0, listStyle: 'none' }}>
                {pending.map((r) => <RequestItem key={r.id} r={r} spaceName={names.get(r.coworkingSpaceId ?? 0) ?? null} />)}
              </ul>
            )}
          </section>

          <section className="stack" aria-labelledby="spaces-title">
            <h2 id="spaces-title" className="section-title">Your spaces</h2>
            <div className="grid-cards">
              {mine.map((s) => (
                <Link key={s.id} to={`/manage/spaces/${s.id}`} className="space-card">
                  <Cover spaceId={s.id} name={s.name} tag={pendingBySpace.get(s.id) ? `${pendingBySpace.get(s.id)} to review` : undefined} />
                  <div className="space-card-body">
                    <h3>{s.name ?? 'Unnamed space'}</h3>
                    <span className="meta">{s.address || 'Address not listed'}</span>
                  </div>
                </Link>
              ))}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
