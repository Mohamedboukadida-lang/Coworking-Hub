import { useId, type ComponentPropsWithoutRef } from 'react';

type FieldProps = ComponentPropsWithoutRef<'input'> & {
  label: string;
  error?: string;
  hint?: string;
};

/** A labelled input with its error and hint wired up for assistive technology. */
export function Field({ label, error, hint, id, ...input }: FieldProps) {
  const generated = useId();
  const inputId = id ?? generated;
  const describedBy = [error && `${inputId}-error`, hint && `${inputId}-hint`].filter(Boolean).join(' ') || undefined;
  return (
    <div className="field">
      <label htmlFor={inputId}>{label}</label>
      <input
        id={inputId}
        className="input"
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...input}
      />
      {hint && !error && <span id={`${inputId}-hint`} className="field-hint">{hint}</span>}
      {error && <span id={`${inputId}-error`} className="field-error">{error}</span>}
    </div>
  );
}
