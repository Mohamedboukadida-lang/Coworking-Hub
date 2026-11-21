import { createContext, useCallback, useContext, useMemo, useSyncExternalStore, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { apiJson } from '../api/client';
import { useMe, keys } from '../api/hooks';
import { authResponse, type Account, type AccountType } from '../api/schemas';
import { getToken, setToken, subscribe } from '../api/tokenStore';

export interface RegisterInput {
  name: string;
  surname: string;
  email: string;
  password: string;
  profession: string;
  typeCompte: Extract<AccountType, 'USER' | 'OWNER'>;
}

type Session =
  | { status: 'anonymous' }
  | { status: 'loading' }
  | { status: 'authenticated'; user: Account };

interface AuthValue {
  session: Session;
  login: (email: string, password: string) => Promise<Account>;
  register: (input: RegisterInput) => Promise<Account>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const token = useSyncExternalStore(subscribe, getToken, () => null);
  const qc = useQueryClient();
  const me = useMe(token !== null);

  const startSession = useCallback(
    (token: string, user: Account) => {
      setToken(token);
      qc.setQueryData(keys.me, user);
      return user;
    },
    [qc],
  );

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await apiJson('/api/auth/login', authResponse, { method: 'POST', body: { email, password } });
      return startSession(res.token, res.user);
    },
    [startSession],
  );

  const register = useCallback(
    async (input: RegisterInput) => {
      const res = await apiJson('/api/auth/register', authResponse, { method: 'POST', body: input });
      return startSession(res.token, res.user);
    },
    [startSession],
  );

  const logout = useCallback(() => {
    setToken(null);
    // Drop everything that belonged to this account.
    qc.removeQueries({ queryKey: keys.me });
    qc.removeQueries({ queryKey: ['reservations'] });
  }, [qc]);

  const session: Session =
    token === null
      ? { status: 'anonymous' }
      : me.data
        ? { status: 'authenticated', user: me.data }
        : me.isError
          ? { status: 'anonymous' }
          : { status: 'loading' };

  const value = useMemo(() => ({ session, login, register, logout }), [session, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside <AuthProvider>');
  return value;
}

/** The signed-in account, or null. */
export function useCurrentUser(): Account | null {
  const { session } = useAuth();
  return session.status === 'authenticated' ? session.user : null;
}

export function canManageSpaces(user: Account | null): boolean {
  return user?.typeCompte === 'OWNER' || user?.typeCompte === 'ADMIN';
}
