import { useState, type ReactNode } from 'react';

/** A destructive action that asks for a second click instead of opening a dialog. */
export function ConfirmButton({ label, confirmLabel, onConfirm, pending, icon }: {
  label: string;
  confirmLabel: string;
  onConfirm: () => void;
  pending?: boolean;
  icon?: ReactNode;
}) {
  const [armed, setArmed] = useState(false);
  if (!armed) {
    return (
      <button type="button" className="btn btn-sm btn-danger" onClick={() => setArmed(true)} disabled={pending}>
        {icon}{label}
      </button>
    );
  }
  return (
    <span className="row" style={{ gap: 6 }}>
      <button type="button" className="btn btn-sm btn-danger" onClick={onConfirm} disabled={pending} autoFocus>
        {pending ? 'Working…' : confirmLabel}
      </button>
      <button type="button" className="btn btn-sm btn-ghost" onClick={() => setArmed(false)} disabled={pending}>Keep</button>
    </span>
  );
}
