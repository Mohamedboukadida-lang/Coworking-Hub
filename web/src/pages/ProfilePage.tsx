import { type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { useUpdateProfile } from '../api/hooks';
import type { Account } from '../api/schemas';
import { useCurrentUser } from '../auth/AuthContext';
import { Field } from '../components/Field';
import { initials } from '../components/Layout';
import { Notice } from '../components/States';

const TYPE_LABELS: Record<NonNullable<Account['typeCompte']>, string> = { USER: 'Member', OWNER: 'Space owner', ADMIN: 'Administrator' };

export function ProfilePage() {
  const user = useCurrentUser();
  if (!user) return null;
  return <ProfileForm user={user} />;
}

function ProfileForm({ user }: { user: Account }) {
  const update = useUpdateProfile(user.id);

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const age = String(form.get('age') ?? '');
    update.mutate({
      name: String(form.get('name') ?? '').trim(),
      surname: String(form.get('surname') ?? '').trim(),
      profession: String(form.get('profession') ?? '').trim(),
      age: age === '' ? undefined : Number(age),
    });
  }

  return (
    <div className="container page" style={{ maxWidth: 680 }}>
      <div className="page-head">
        <span className="avatar" style={{ width: 64, height: 64, fontSize: 22 }} aria-hidden="true">
          {initials(`${user.name ?? ''} ${user.surname ?? ''}`)}
        </span>
        <div className="stack" style={{ gap: 4 }}>
          <h1 className="display">{[user.name, user.surname].filter(Boolean).join(' ') || 'Your profile'}</h1>
          <p className="muted">{user.email} · {user.typeCompte ? TYPE_LABELS[user.typeCompte] : 'Member'}</p>
        </div>
      </div>
      <form className="card card-pad stack" onSubmit={submit}>
        {update.isError && <Notice tone="error">{errorMessage(update.error)}</Notice>}
        {update.isSuccess && <Notice tone="success">Profile saved.</Notice>}
        <div className="form-grid">
          <Field label="First name" name="name" defaultValue={user.name ?? ''} autoComplete="given-name" />
          <Field label="Last name" name="surname" defaultValue={user.surname ?? ''} autoComplete="family-name" />
        </div>
        <div className="form-grid">
          <Field label="Profession" name="profession" defaultValue={user.profession ?? ''} autoComplete="organization-title" />
          <Field label="Age" name="age" type="number" inputMode="numeric" min={16} max={120} defaultValue={user.age ?? ''} />
        </div>
        <div className="row"><span className="spacer" />
          <button type="submit" className="btn btn-primary" disabled={update.isPending}>{update.isPending ? 'Saving…' : 'Save profile'}</button>
        </div>
      </form>
    </div>
  );
}
