import { ArrowLeft, ExternalLink, PackagePlus, Plus, ShieldAlert, Trash2 } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import {
  useAddRoom,
  useDeleteSpace,
  useEquipment,
  useRooms,
  useSpace,
  useSpaceReservations,
  useStockEquipment,
  useUpdateSpace,
} from '../api/hooks';
import type { Space } from '../api/schemas';
import { useCurrentUser } from '../auth/AuthContext';
import { ConfirmButton } from '../components/ConfirmButton';
import { Field } from '../components/Field';
import { RequestItem } from '../components/RequestItem';
import { EmptyState, ErrorState, Notice, Skeleton } from '../components/States';
import { EQUIPMENT_LABELS, formatMoney } from '../lib/format';

export function ManageSpacePage() {
  const id = Number(useParams().id);
  const space = useSpace(id);
  const user = useCurrentUser();

  if (space.isPending) return <div className="container page"><Skeleton height={320} /></div>;
  if (space.isError) return <ErrorState message={errorMessage(space.error)} onRetry={() => space.refetch()} />;
  const s = space.data;
  const canManage = user?.typeCompte === 'ADMIN' || s.ownerId === user?.id;
  if (!canManage) {
    return (
      <EmptyState icon={<ShieldAlert size={24} />} title="Not your space"
        action={<Link className="btn" to="/manage">Back to your dashboard</Link>}>
        Only the owner of {s.name ?? 'this space'} can manage it.
      </EmptyState>
    );
  }

  return (
    <div className="container page">
      <Link to="/manage" className="btn btn-ghost btn-sm" style={{ marginBottom: 'var(--space-4)' }}>
        <ArrowLeft size={15} aria-hidden="true" /> Dashboard
      </Link>
      <div className="page-head">
        <div className="stack" style={{ gap: 6 }}>
          <p className="eyebrow">Manage</p>
          <h1 className="display">{s.name ?? 'Unnamed space'}</h1>
        </div>
        <span className="spacer" />
        <Link to={`/spaces/${s.id}`} className="btn"><ExternalLink size={15} aria-hidden="true" />View public page</Link>
      </div>
      <div className="stack-lg">
        <Requests spaceId={s.id} spaceName={s.name} />
        <div className="two-col">
          <Rooms spaceId={s.id} />
          <div className="stack-lg">
            <EquipmentSection spaceId={s.id} />
            <Details space={s} />
          </div>
        </div>
      </div>
    </div>
  );
}

function Requests({ spaceId, spaceName }: { spaceId: number; spaceName: string | null }) {
  const [query] = useSpaceReservations([spaceId]);
  const list = [...(query?.data ?? [])].sort((a, b) => {
    const rank = (s: string | null) => (s === 'PENDING' ? 0 : 1);
    return rank(a.status) - rank(b.status) || `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`);
  });
  return (
    <section className="stack" aria-labelledby="requests-title">
      <h2 id="requests-title" className="section-title">Bookings</h2>
      {query?.isPending && <Skeleton height={120} />}
      {query?.isError && <Notice tone="error">{errorMessage(query.error)}</Notice>}
      {query?.isSuccess && list.length === 0 && <p className="muted">No bookings yet.</p>}
      {list.length > 0 && (
        <ul className="card list" style={{ padding: 0, margin: 0, listStyle: 'none' }}>
          {list.map((r) => <RequestItem key={r.id} r={r} spaceName={spaceName} />)}
        </ul>
      )}
    </section>
  );
}

function Rooms({ spaceId }: { spaceId: number }) {
  const rooms = useRooms(spaceId);
  const add = useAddRoom(spaceId);
  const [errors, setErrors] = useState<{ capacity?: string; price?: string }>({});

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const formEl = e.currentTarget;
    const form = new FormData(formEl);
    const capacity = Number(form.get('capacity'));
    const price = Number(form.get('price'));
    const found: typeof errors = {};
    if (!Number.isInteger(capacity) || capacity < 1) found.capacity = 'Whole number, at least 1.';
    if (!Number.isFinite(price) || price < 0 || String(form.get('price')) === '') found.price = 'Zero or more.';
    setErrors(found);
    if (found.capacity || found.price) return;
    add.mutate({ capacity, pricePerPersonPerHour: price }, { onSuccess: () => formEl.reset() });
  }

  const list = [...(rooms.data ?? [])].sort((a, b) => a.capacity - b.capacity);
  return (
    <section className="card" aria-labelledby="rooms-title">
      <div className="card-pad stack" style={{ paddingBottom: 'var(--space-4)' }}>
        <h2 id="rooms-title" className="section-title">Rooms &amp; prices</h2>
        <p className="muted small">Each booking goes to the cheapest free room that fits the group.</p>
      </div>
      {rooms.isPending && <div className="card-pad"><Skeleton height={80} /></div>}
      {rooms.isSuccess && list.length === 0 && (
        <div className="card-pad" style={{ paddingTop: 0 }}><Notice tone="info">Add at least one room so members can book.</Notice></div>
      )}
      {list.length > 0 && (
        <table className="table">
          <thead><tr><th scope="col">Seats</th><th scope="col">Price / person / hour</th></tr></thead>
          <tbody>
            {list.map((r) => (
              <tr key={r.id}><td>{r.capacity}</td><td>{formatMoney(r.pricePerPersonPerHour)}</td></tr>
            ))}
          </tbody>
        </table>
      )}
      <hr className="divider" />
      <form className="card-pad stack" onSubmit={submit} noValidate aria-label="Add a room">
        {add.isError && <Notice tone="error">{errorMessage(add.error)}</Notice>}
        <div className="form-grid">
          <Field label="Seats" name="capacity" type="number" inputMode="numeric" min={1} placeholder="4" error={errors.capacity} />
          <Field label="Price (TND)" name="price" type="number" inputMode="decimal" min={0} step="0.5" placeholder="10" error={errors.price} />
        </div>
        <button type="submit" className="btn" disabled={add.isPending}><Plus size={16} aria-hidden="true" />{add.isPending ? 'Adding…' : 'Add room'}</button>
      </form>
    </section>
  );
}

function EquipmentSection({ spaceId }: { spaceId: number }) {
  const equipment = useEquipment(spaceId);
  const stock = useStockEquipment(spaceId);
  return (
    <section className="card card-pad stack" aria-labelledby="equipment-title">
      <h2 id="equipment-title" className="section-title">Equipment</h2>
      {stock.isError && <Notice tone="error">{errorMessage(stock.error)}</Notice>}
      {equipment.data && equipment.data.length > 0 ? (
        <div className="chips">
          {equipment.data.map((e) => (
            <span key={e.id} className="chip" style={{ cursor: 'default' }}>
              {e.typeOfEquipment ? EQUIPMENT_LABELS[e.typeOfEquipment] : e.name}
              <span className="chip-price">{formatMoney(e.price)}/h</span>
            </span>
          ))}
        </div>
      ) : (
        equipment.isSuccess && <p className="muted">No equipment yet.</p>
      )}
      <button type="button" className="btn" onClick={() => stock.mutate()} disabled={stock.isPending}>
        <PackagePlus size={16} aria-hidden="true" />{stock.isPending ? 'Adding…' : 'Add a starter set (one of each)'}
      </button>
    </section>
  );
}

function Details({ space }: { space: Space }) {
  const update = useUpdateSpace(space.id);
  const remove = useDeleteSpace();
  const navigate = useNavigate();
  return (
    <section className="card card-pad stack" aria-labelledby="details-title">
      <h2 id="details-title" className="section-title">Details</h2>
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          const form = new FormData(e.currentTarget);
          update.mutate({ name: String(form.get('name') ?? '').trim() || undefined, address: String(form.get('address') ?? '').trim() });
        }}
      >
        {update.isError && <Notice tone="error">{errorMessage(update.error)}</Notice>}
        {update.isSuccess && <Notice tone="success">Saved.</Notice>}
        <Field label="Name" name="name" defaultValue={space.name ?? ''} />
        <Field label="Address" name="address" defaultValue={space.address ?? ''} />
        <button type="submit" className="btn" disabled={update.isPending}>{update.isPending ? 'Saving…' : 'Save changes'}</button>
      </form>
      <hr className="divider" />
      <div className="stack" style={{ gap: 8 }}>
        <span className="label small" style={{ fontWeight: 600 }}>Delete this space</span>
        {remove.isError && <Notice tone="error">{errorMessage(remove.error)} Remove its rooms, equipment and bookings first.</Notice>}
        <div>
          <ConfirmButton label="Delete space" confirmLabel="Delete permanently" icon={<Trash2 size={14} aria-hidden="true" />}
            pending={remove.isPending} onConfirm={() => remove.mutate(space.id, { onSuccess: () => navigate('/manage') })} />
        </div>
      </div>
    </section>
  );
}
