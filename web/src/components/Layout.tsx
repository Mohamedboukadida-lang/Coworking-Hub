import { Building2, CalendarCheck, Compass, LayoutDashboard, LogOut } from 'lucide-react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { canManageSpaces, useAuth } from '../auth/AuthContext';

export function initials(name: string | null | undefined): string {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
  return ((parts[0]?.[0] ?? '?') + (parts[1]?.[0] ?? '')).toUpperCase();
}

export function Layout() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();
  const user = session.status === 'authenticated' ? session.user : null;

  return (
    <>
      <a className="sr-only" href="#main">Skip to content</a>
      <header className="header">
        <div className="container header-inner">
          <Link to="/" className="brand" aria-label="Coworking Hub home">
            <span className="brand-mark" aria-hidden="true"><Building2 size={18} /></span>
            <span>Coworking Hub</span>
          </Link>
          <nav className="nav" aria-label="Main">
            <NavLink to="/" end><Compass size={16} aria-hidden="true" />Explore</NavLink>
            {user && <NavLink to="/bookings"><CalendarCheck size={16} aria-hidden="true" />My bookings</NavLink>}
            {canManageSpaces(user) && (
              <NavLink to="/manage"><LayoutDashboard size={16} aria-hidden="true" />Manage</NavLink>
            )}
          </nav>
          <div className="spacer" />
          {session.status === 'loading' && <span className="muted small">Loading…</span>}
          {session.status === 'anonymous' && (
            <div className="row">
              <Link to="/login" className="btn btn-ghost">Log in</Link>
              <Link to="/register" className="btn btn-primary">Sign up</Link>
            </div>
          )}
          {user && (
            <div className="row">
              <Link to="/profile" className="user-chip" aria-label={`Profile of ${user.name ?? 'you'}`}>
                <span className="avatar" aria-hidden="true">{initials(`${user.name ?? ''} ${user.surname ?? ''}`)}</span>
                <span className="user-name">{user.name}</span>
              </Link>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  logout();
                  navigate('/');
                }}
              >
                <LogOut size={15} aria-hidden="true" />
                <span>Log out</span>
              </button>
            </div>
          )}
        </div>
      </header>
      <main id="main">
        <Outlet />
      </main>
      <footer className="footer">
        <div className="container row">
          <span>© {new Date().getFullYear()} Coworking Hub</span>
          <span className="spacer" />
          <span>Find, compare and book workspaces.</span>
        </div>
      </footer>
    </>
  );
}
