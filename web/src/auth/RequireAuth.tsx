import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { canManageSpaces, useAuth } from './AuthContext';
import { EmptyState, Skeleton } from '../components/States';

/** Sends anonymous visitors to the login page and back afterwards. Optionally requires an owner/admin. */
export function RequireAuth({ children, owner = false }: { children: ReactNode; owner?: boolean }) {
  const { session } = useAuth();
  const location = useLocation();
  if (session.status === 'loading') {
    return <div className="container page"><Skeleton height={200} /></div>;
  }
  if (session.status === 'anonymous') {
    return <Navigate to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }
  if (owner && !canManageSpaces(session.user)) {
    return (
      <EmptyState icon={<ShieldAlert size={24} />} title="For space owners" action={<Link className="btn" to="/">Explore spaces</Link>}>
        Managing spaces needs an owner account. Sign up as an owner to list your own space.
      </EmptyState>
    );
  }
  return <>{children}</>;
}
