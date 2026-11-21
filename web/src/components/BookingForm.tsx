import { CalendarDays, Clock } from 'lucide-react';
import { useId, useState, type FormEvent } from 'react';
import type { Equipment, Room } from '../api/schemas';
import { EQUIPMENT_LABELS, formatMoney, latestStart, todayIso } from '../lib/format';
import { estimate } from '../lib/pricing';
import { Field } from './Field';

export interface BookingValues {
  date: string;
  time: string;
  hours: number;
  numberOfPeople: number;
  equipmentIds: number[];
}

interface BookingFormProps {
  rooms: Room[];
  equipment: Equipment[];
  initialPeople?: number;
  pending: boolean;
  onSubmit: (values: BookingValues) => void;
  /** Rendered in place of the submit button, e.g. a "log in to book" link. */
  submitSlot?: React.ReactNode;
}

type Errors = Partial<Record<'date' | 'time' | 'people', string>>;

function validate(date: string, time: string, hours: number, people: number): Errors {
  const errors: Errors = {};
  if (!date) errors.date = 'Choose a date.';
  else if (date < todayIso()) errors.date = 'Choose today or a later date.';
  if (!/^([01]?\d|2[0-3]):[0-5]\d$/.test(time)) errors.time = 'Choose a start time.';
  else {
    const [h = 0, m = 0] = time.split(':').map(Number);
    if (h * 60 + m + hours * 60 > 24 * 60) errors.time = `Must end by midnight: start by ${latestStart(hours)}.`;
  }
  if (!Number.isInteger(people) || people < 1) errors.people = 'At least one person.';
  return errors;
}

export function BookingForm({ rooms, equipment, initialPeople = 1, pending, onSubmit, submitSlot }: BookingFormProps) {
  const [date, setDate] = useState('');
  const [time, setTime] = useState('09:00');
  const [hours, setHours] = useState(2);
  const [people, setPeople] = useState(initialPeople);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [errors, setErrors] = useState<Errors>({});
  const totalLabelId = useId();

  const chosen = equipment.filter((e) => selected.has(e.id));
  const quote = estimate(rooms, people, hours, chosen.map((e) => e.price));

  function toggle(id: number) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const found = validate(date, time, hours, people);
    setErrors(found);
    const firstInvalid = (['date', 'time', 'people'] as const).find((k) => found[k]);
    if (firstInvalid) {
      e.currentTarget.querySelector<HTMLInputElement>(`[name="${firstInvalid}"]`)?.focus();
      return;
    }
    if (quote.kind !== 'ok') return;
    onSubmit({ date, time, hours, numberOfPeople: people, equipmentIds: [...selected] });
  }

  return (
    <form className="stack" onSubmit={submit} noValidate aria-label="Book this space">
      <div className="form-grid">
        <Field label="Date" name="date" type="date" min={todayIso()} value={date}
          onChange={(e) => setDate(e.target.value)} error={errors.date} required />
        <Field label="Start time" name="time" type="time" step={900} value={time}
          onChange={(e) => setTime(e.target.value)} error={errors.time} required />
      </div>
      <div className="form-grid">
        <div className="field">
          <label htmlFor="hours">Duration</label>
          <select id="hours" name="hours" className="input" value={hours} onChange={(e) => setHours(Number(e.target.value))}>
            {[1, 2, 3, 4, 5, 6, 8, 10, 12].map((h) => <option key={h} value={h}>{h} {h === 1 ? 'hour' : 'hours'}</option>)}
          </select>
        </div>
        <Field label="People" name="people" type="number" inputMode="numeric" min={1} max={50} value={people}
          onChange={(e) => setPeople(Number(e.target.value))} error={errors.people} required />
      </div>

      {equipment.length > 0 && (
        <fieldset className="field" style={{ border: 0, padding: 0, margin: 0 }}>
          <legend className="label" style={{ marginBottom: 8 }}>Add equipment <span className="muted">(per hour)</span></legend>
          <div className="chips">
            {equipment.map((item) => (
              <label key={item.id} className="chip">
                <input type="checkbox" checked={selected.has(item.id)} onChange={() => toggle(item.id)} />
                {item.typeOfEquipment ? EQUIPMENT_LABELS[item.typeOfEquipment] : item.name}
                <span className="chip-price">+{item.price.toFixed(2)}</span>
              </label>
            ))}
          </div>
        </fieldset>
      )}

      <div className="summary" aria-live="polite">
        {quote.kind === 'ok' ? (
          <>
            <div className="summary-row">
              <span>Room for up to {quote.room.capacity} · {formatMoney(quote.room.pricePerPersonPerHour)} × {people} × {hours}h</span>
              <span>{formatMoney(quote.room.pricePerPersonPerHour * people * hours)}</span>
            </div>
            {chosen.length > 0 && (
              <div className="summary-row">
                <span>Equipment · {chosen.length} {chosen.length === 1 ? 'item' : 'items'} × {hours}h</span>
                <span>{formatMoney(chosen.reduce((s, e) => s + e.price, 0) * hours)}</span>
              </div>
            )}
            <div className="summary-total">
              <span className="muted small" id={totalLabelId}>Estimated total</span>
              <output aria-labelledby={totalLabelId}><strong>{formatMoney(quote.total)}</strong></output>
            </div>
          </>
        ) : (
          <span className="small">
            {rooms.length === 0
              ? 'This space has no rooms to book yet.'
              : `No room here seats ${people} people (largest: ${quote.maxCapacity}).`}
          </span>
        )}
      </div>

      {submitSlot ?? (
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={pending || quote.kind !== 'ok'}>
          {pending ? 'Booking…' : 'Request booking'}
        </button>
      )}
      <p className="muted small row" style={{ gap: 6 }}>
        <CalendarDays size={14} aria-hidden="true" />
        <span>The owner confirms each request.</span>
        <Clock size={14} aria-hidden="true" />
        <span>If the cheapest room is taken, the next free one is used.</span>
      </p>
    </form>
  );
}
