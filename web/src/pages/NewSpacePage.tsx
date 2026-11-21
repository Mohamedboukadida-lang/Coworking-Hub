import { ArrowLeft } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useCreateSpace } from '../api/hooks';
import { Field } from '../components/Field';
import { LocationPicker } from '../components/SpaceMap';
import { Notice } from '../components/States';

export function NewSpacePage() {
  const create = useCreateSpace();
  const navigate = useNavigate();
  const [location, setLocation] = useState<[number, number] | null>(null);
  const [nameError, setNameError] = useState<string>();

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const name = String(form.get('name') ?? '').trim();
    if (!name) {
      setNameError('Give your space a name.');
      e.currentTarget.querySelector<HTMLInputElement>('[name="name"]')?.focus();
      return;
    }
    setNameError(undefined);
    create.mutate(
      {
        name,
        address: String(form.get('address') ?? '').trim(),
        coordinateX: location?.[0] ?? 0,
        coordinateY: location?.[1] ?? 0,
      },
      { onSuccess: (created) => navigate(`/manage/spaces/${created.id}`) },
    );
  }

  return (
    <div className="container page" style={{ maxWidth: 760 }}>
      <Link to="/manage" className="btn btn-ghost btn-sm" style={{ marginBottom: 'var(--space-4)' }}>
        <ArrowLeft size={15} aria-hidden="true" /> Dashboard
      </Link>
      <div className="page-head">
        <div className="stack" style={{ gap: 6 }}>
          <p className="eyebrow">New listing</p>
          <h1 className="display">Add a coworking space</h1>
        </div>
      </div>
      <form className="card card-pad stack" onSubmit={submit} noValidate>
        {create.isError && <Notice tone="error">{errorMessage(create.error)}</Notice>}
        <Field label="Name" name="name" placeholder="e.g. Hub Lac" error={nameError} required />
        <Field label="Address" name="address" placeholder="Street, neighbourhood, city" autoComplete="street-address" />
        <div className="field">
          <span className="label" id="location-label">Location</span>
          <LocationPicker label="Click the map to place your space" value={location} onChange={(lat, lng) => setLocation([lat, lng])} />
          <span className="field-hint">
            {location ? `Pinned at ${location[0].toFixed(4)}, ${location[1].toFixed(4)}.` : 'Click the map to place your space so members can find it.'}
          </span>
        </div>
        <div className="row">
          <span className="spacer" />
          <Link to="/manage" className="btn btn-ghost">Cancel</Link>
          <button type="submit" className="btn btn-primary" disabled={create.isPending}>
            {create.isPending ? 'Creating…' : 'Create space'}
          </button>
        </div>
        <p className="muted small">Next you'll add rooms and prices.</p>
      </form>
    </div>
  );
}
