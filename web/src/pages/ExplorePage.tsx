import { MapPin, Search, Users } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useRoomsForSpaces, useSpaces } from '../api/hooks';
import type { Room } from '../api/schemas';
import { Cover } from '../components/Cover';
import { SpaceMap } from '../components/SpaceMap';
import { EmptyState, ErrorState, Skeleton } from '../components/States';
import { formatMoney } from '../lib/format';
import { fromPrice } from '../lib/pricing';

export function ExplorePage() {
  const [params, setParams] = useSearchParams();
  const query = params.get('q') ?? '';
  const people = Number(params.get('people') ?? '1') || 1;
  const [hovered, setHovered] = useState<number | null>(null);
  const navigate = useNavigate();

  const spaces = useSpaces();
  const all = spaces.data ?? [];
  const roomQueries = useRoomsForSpaces(all.map((s) => s.id));
  const roomsBySpace = new Map<number, Room[] | undefined>(all.map((s, i) => [s.id, roomQueries[i]?.data]));

  const needle = query.trim().toLowerCase();
  const visible = all.filter((s) => {
    const matchesText = !needle || `${s.name ?? ''} ${s.address ?? ''}`.toLowerCase().includes(needle);
    const rooms = roomsBySpace.get(s.id);
    // Until rooms have loaded, keep the space; afterwards hide spaces that cannot seat the party.
    const fits = rooms === undefined || rooms.some((r) => r.capacity >= people);
    return matchesText && fits;
  });

  const priceLabels: Record<number, string> = {};
  for (const s of visible) {
    const price = fromPrice(roomsBySpace.get(s.id) ?? []);
    if (price !== null) priceLabels[s.id] = `${price.toFixed(0)} TND`;
  }

  function update(next: { q?: string; people?: number }) {
    const merged = new URLSearchParams(params);
    if (next.q !== undefined) {
      if (next.q) merged.set('q', next.q);
      else merged.delete('q');
    }
    if (next.people !== undefined) {
      if (next.people > 1) merged.set('people', String(next.people));
      else merged.delete('people');
    }
    setParams(merged, { replace: true });
  }

  return (
    <div className="container">
      <section className="hero">
        <div className="hero-copy">
        <p className="eyebrow">Workspaces in Tunis and beyond</p>
        <h1 className="display">Find a desk that <em>works</em> for you.</h1>
        <p className="lead">
          Compare coworking spaces, see live room prices, and book a desk or a whole team room in seconds.
        </p>
        <form className="search" role="search" onSubmit={(e) => e.preventDefault()}>
          <label>
            <Search size={18} aria-hidden="true" className="muted" />
            <span className="sr-only">Search by name or address</span>
            <input
              type="search"
              placeholder="Search by name or neighbourhood"
              value={query}
              onChange={(e) => update({ q: e.target.value })}
            />
          </label>
          <label>
            <Users size={18} aria-hidden="true" className="muted" />
            <span className="sr-only">Number of people</span>
            <select value={people} onChange={(e) => update({ people: Number(e.target.value) })}>
              {[1, 2, 3, 4, 6, 8, 10, 12].map((n) => (
                <option key={n} value={n}>{n === 1 ? '1 person' : `${n} people`}</option>
              ))}
            </select>
          </label>
        </form>
        </div>
        {all.length > 0 && (
          <div className="hero-art" aria-hidden="true">
            {all.slice(0, 2).map((s, i) => {
              const price = fromPrice(roomsBySpace.get(s.id) ?? []);
              return (
                <div key={s.id} className={`hero-card hero-card-${i}`}>
                  <Cover spaceId={s.id} name={s.name} />
                  <div className="hero-card-body">
                    <strong>{s.name}</strong>
                    {price !== null && <span>from {formatMoney(price)}/h</span>}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {spaces.isPending && (
        <div className="explore page" aria-busy="true">
          <div className="grid-cards">
            {[0, 1, 2, 3].map((i) => <Skeleton key={i} height={260} />)}
          </div>
          <Skeleton height={420} />
        </div>
      )}

      {spaces.isError && <ErrorState message={errorMessage(spaces.error)} onRetry={() => spaces.refetch()} />}

      {spaces.isSuccess && all.length === 0 && (
        <EmptyState icon={<MapPin size={24} />} title="No spaces listed yet">
          Space owners can sign up and list their first space from the Manage page.
        </EmptyState>
      )}

      {spaces.isSuccess && all.length > 0 && (
        <div className="explore" style={{ paddingBottom: 'var(--space-8)' }}>
          <section aria-labelledby="results-title" className="stack">
            <div className="row">
              <h2 id="results-title" className="section-title">
                {visible.length} {visible.length === 1 ? 'space' : 'spaces'}
                {people > 1 ? ` for ${people} people` : ''}
              </h2>
            </div>
            {visible.length === 0 ? (
              <EmptyState icon={<Search size={24} />} title="No matches">
                Try another search or a smaller group.
              </EmptyState>
            ) : (
              <div className="grid-cards">
                {visible.map((s) => {
                  const rooms = roomsBySpace.get(s.id);
                  const price = fromPrice(rooms ?? []);
                  const seats = rooms ? Math.max(0, ...rooms.map((r) => r.capacity)) : null;
                  return (
                    <Link
                      key={s.id}
                      to={`/spaces/${s.id}${people > 1 ? `?people=${people}` : ''}`}
                      className="space-card"
                      data-active={hovered === s.id}
                      onMouseEnter={() => setHovered(s.id)}
                      onMouseLeave={() => setHovered(null)}
                      onFocus={() => setHovered(s.id)}
                      onBlur={() => setHovered(null)}
                    >
                      <Cover spaceId={s.id} name={s.name} tag={rooms ? `${rooms.length} ${rooms.length === 1 ? 'room' : 'rooms'}` : undefined} />
                      <div className="space-card-body">
                        <h3>{s.name ?? 'Unnamed space'}</h3>
                        <span className="meta"><MapPin size={14} aria-hidden="true" />{s.address || 'Address not listed'}</span>
                        <div className="row" style={{ marginTop: 6 }}>
                          {price !== null ? (
                            <span className="price">{formatMoney(price)} <small>/ person / hour</small></span>
                          ) : (
                            <span className="muted small">{rooms ? 'No rooms yet' : 'Loading prices…'}</span>
                          )}
                          <span className="spacer" />
                          {seats ? <span className="meta"><Users size={14} aria-hidden="true" />up to {seats}</span> : null}
                        </div>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </section>
          <div className="explore-map">
            <SpaceMap
              label="Map of coworking spaces"
              spaces={visible}
              labels={priceLabels}
              activeId={hovered}
              onHover={setHovered}
              onSelect={(id) => navigate(`/spaces/${id}`)}
            />
          </div>
        </div>
      )}
    </div>
  );
}
