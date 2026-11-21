import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth, type RegisterInput } from '../auth/AuthContext';
import { Field } from '../components/Field';
import { Notice } from '../components/States';

/** Only same-site paths are allowed as a post-login destination. */
function safeNext(next: string | null): string {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/';
}

function AuthShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="auth">
      <div className="auth-art" aria-hidden="true">
        <h2>Your next good workday starts here.</h2>
        <p>Desks, team rooms and the equipment you need, booked in a minute.</p>
      </div>
      <div className="auth-form"><div className="stack-lg">{children}</div></div>
    </div>
  );
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    setPending(true);
    setError(null);
    try {
      await login(String(form.get('email')), String(form.get('password')));
      navigate(safeNext(params.get('next')), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setPending(false);
    }
  }

  return (
    <AuthShell>
      <header className="stack" style={{ gap: 8 }}>
        <h1 className="display" style={{ fontSize: 40 }}>Welcome back</h1>
        <p className="muted">Log in to book and manage your reservations.</p>
      </header>
      {error && <Notice tone="error">{error}</Notice>}
      <form className="stack" onSubmit={submit}>
        <Field label="Email" name="email" type="email" autoComplete="email" required />
        <Field label="Password" name="password" type="password" autoComplete="current-password" required />
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={pending}>
          {pending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="muted">
        New here? <Link to={`/register${params.get('next') ? `?next=${encodeURIComponent(params.get('next') ?? '')}` : ''}`}>Create an account</Link>
      </p>
    </AuthShell>
  );
}

type FieldErrors = Partial<Record<'name' | 'email' | 'password', string>>;

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [type, setType] = useState<RegisterInput['typeCompte']>('USER');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [pending, setPending] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const input: RegisterInput = {
      name: String(form.get('name') ?? '').trim(),
      surname: String(form.get('surname') ?? '').trim(),
      email: String(form.get('email') ?? '').trim(),
      password: String(form.get('password') ?? ''),
      profession: String(form.get('profession') ?? '').trim(),
      typeCompte: type,
    };
    const found: FieldErrors = {};
    if (!input.name) found.name = 'Enter your first name.';
    if (!/^\S+@\S+\.\S+$/.test(input.email)) found.email = 'Enter a valid email address.';
    if (input.password.length < 8) found.password = 'Use at least 8 characters.';
    setFieldErrors(found);
    const first = (['name', 'email', 'password'] as const).find((k) => found[k]);
    if (first) {
      e.currentTarget.querySelector<HTMLInputElement>(`[name="${first}"]`)?.focus();
      return;
    }
    setPending(true);
    setError(null);
    try {
      const user = await register(input);
      const fallback = user.typeCompte === 'OWNER' ? '/manage' : '/';
      const next = params.get('next');
      navigate(next ? safeNext(next) : fallback, { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setPending(false);
    }
  }

  return (
    <AuthShell>
      <header className="stack" style={{ gap: 8 }}>
        <h1 className="display" style={{ fontSize: 40 }}>Create your account</h1>
        <p className="muted">Book spaces as a member, or list your own as an owner.</p>
      </header>
      {error && <Notice tone="error">{error}</Notice>}
      <form className="stack" onSubmit={submit} noValidate>
        <fieldset style={{ border: 0, padding: 0, margin: 0 }}>
          <legend className="sr-only">Account type</legend>
          <div className="segmented">
            <label>
              <input type="radio" name="type" value="USER" checked={type === 'USER'} onChange={() => setType('USER')} />
              <strong>Member</strong>I want to book spaces
            </label>
            <label>
              <input type="radio" name="type" value="OWNER" checked={type === 'OWNER'} onChange={() => setType('OWNER')} />
              <strong>Owner</strong>I want to list a space
            </label>
          </div>
        </fieldset>
        <div className="form-grid">
          <Field label="First name" name="name" autoComplete="given-name" error={fieldErrors.name} required />
          <Field label="Last name" name="surname" autoComplete="family-name" />
        </div>
        <Field label="Profession" name="profession" autoComplete="organization-title" hint="Optional — shown on your profile." />
        <Field label="Email" name="email" type="email" autoComplete="email" error={fieldErrors.email} required />
        <Field label="Password" name="password" type="password" autoComplete="new-password" error={fieldErrors.password}
          hint="At least 8 characters." required />
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={pending}>
          {pending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="muted">Already have an account? <Link to="/login">Log in</Link></p>
    </AuthShell>
  );
}
