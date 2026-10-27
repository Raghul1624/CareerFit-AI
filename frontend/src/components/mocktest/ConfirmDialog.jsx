import { useEffect, useRef } from 'react';
import { Button } from '../ui.jsx';

/** Simple modal confirmation (replaces window.confirm so it can show the answered/unanswered counts). */
export default function ConfirmDialog({ open, title, children, confirmLabel = 'Confirm', cancelLabel = 'Cancel', busy, onConfirm, onCancel }) {
  const cancelRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    cancelRef.current?.focus();
    const onKey = (e) => { if (e.key === 'Escape') onCancel(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onCancel]);

  if (!open) return null;
  return (
    <div className="cf-modal-backdrop" role="presentation" onClick={onCancel}>
      <div className="cf-modal cf-card" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        <div>{children}</div>
        <div className="cf-modal-actions">
          <button type="button" ref={cancelRef} className="cf-btn cf-btn-outline" onClick={onCancel} disabled={busy}>{cancelLabel}</button>
          <Button onClick={onConfirm} loading={busy}>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}
