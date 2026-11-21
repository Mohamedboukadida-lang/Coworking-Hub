import { AlertTriangle, CheckCircle2, Info } from 'lucide-react';
import type { ReactNode } from 'react';

export function EmptyState({ icon, title, children, action }: {
  icon: ReactNode;
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="state">
      <div className="state-icon" aria-hidden="true">{icon}</div>
      <h2>{title}</h2>
      {children && <p>{children}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="state" role="alert">
      <div className="state-icon" style={{ background: 'var(--danger-soft)', color: 'var(--danger)' }} aria-hidden="true">
        <AlertTriangle size={24} />
      </div>
      <h2>Something went wrong</h2>
      <p>{message}</p>
      {onRetry && <button type="button" className="btn" onClick={onRetry}>Try again</button>}
    </div>
  );
}

type Tone = 'error' | 'success' | 'info';

export function Notice({ tone, children }: { tone: Tone; children: ReactNode }) {
  const Icon = tone === 'error' ? AlertTriangle : tone === 'success' ? CheckCircle2 : Info;
  return (
    <div className={`notice notice-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <Icon size={17} aria-hidden="true" />
      <div>{children}</div>
    </div>
  );
}

export function Skeleton({ height, style }: { height: number | string; style?: React.CSSProperties }) {
  return <div className="skeleton" style={{ height, ...style }} aria-hidden="true" />;
}
