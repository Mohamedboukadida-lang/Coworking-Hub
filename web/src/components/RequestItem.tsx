import { CalendarDays, Check, Clock, Users, X } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { useSetReservationStatus } from '../api/hooks';
import type { Reservation } from '../api/schemas';
import { formatDate, formatMoney, timeRange } from '../lib/format';
import { Cover } from './Cover';
import { StatusBadge } from './StatusBadge';

/** A booking request as the space owner sees it, with accept/decline actions. */
export function RequestItem({ r, spaceName }: { r: Reservation; spaceName: string | null }) {
  const setStatus = useSetReservationStatus();
  const [declining, setDeclining] = useState(false);
  const [reason, setReason] = useState('');
  const spaceId = r.coworkingSpaceId ?? 0;

  return (
    <li className="list-item">
      <Cover spaceId={spaceId} name={spaceName} className="thumb" />
      <div>
        <div className="row" style={{ gap: 10 }}>
          <h3>{spaceName ?? `Space #${spaceId}`}</h3>
          <StatusBadge status={r.status} />
        </div>
        <div className="meta">
          <span><CalendarDays size={14} aria-hidden="true" />{formatDate(r.date)}</span>
          <span><Clock size={14} aria-hidden="true" />{timeRange(r)}</span>
          <span><Users size={14} aria-hidden="true" />{r.numberOfPeople}</span>
          <span className="price" style={{ color: 'var(--ink)' }}>{formatMoney(r.totalPrice)}</span>
        </div>
        {setStatus.isError && <p className="field-error" role="alert" style={{ marginTop: 6 }}>{errorMessage(setStatus.error)}</p>}
        {declining && (
          <form
            className="row"
            style={{ marginTop: 10 }}
            onSubmit={(e) => {
              e.preventDefault();
              setStatus.mutate({ id: r.id, status: 'DECLINED', reason: reason.trim() || undefined }, { onSuccess: () => setDeclining(false) });
            }}
          >
            <label className="sr-only" htmlFor={`reason-${r.id}`}>Reason for declining (optional)</label>
            <input id={`reason-${r.id}`} className="input" style={{ flex: 1, minWidth: 180 }} placeholder="Reason (optional), e.g. closed for an event"
              value={reason} onChange={(e) => setReason(e.target.value)} autoFocus />
            <button type="submit" className="btn btn-sm btn-danger" disabled={setStatus.isPending}>Decline</button>
            <button type="button" className="btn btn-sm btn-ghost" onClick={() => setDeclining(false)}>Back</button>
          </form>
        )}
      </div>
      {!declining && (
        <div className="list-actions">
          {r.status !== 'ACCEPTED' && (
            <button type="button" className="btn btn-sm btn-success" disabled={setStatus.isPending}
              onClick={() => setStatus.mutate({ id: r.id, status: 'ACCEPTED' })}>
              <Check size={14} aria-hidden="true" />Accept
            </button>
          )}
          {r.status !== 'DECLINED' && (
            <button type="button" className="btn btn-sm btn-danger" disabled={setStatus.isPending} onClick={() => setDeclining(true)}>
              <X size={14} aria-hidden="true" />Decline
            </button>
          )}
        </div>
      )}
    </li>
  );
}
