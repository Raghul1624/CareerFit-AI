import { Link } from 'react-router-dom';

export function LoadingSpinner({ label = 'Loading...' }) {
  return (
    <div className="cf-center" role="status">
      <div className="cf-spinner" />
      <p className="cf-muted">{label}</p>
    </div>
  );
}

export function ErrorMessage({ message }) {
  if (!message) return null;
  return <div className="cf-alert cf-alert-error" role="alert">{message}</div>;
}

export function SuccessMessage({ message }) {
  if (!message) return null;
  return <div className="cf-alert cf-alert-success" role="status">{message}</div>;
}

export function EmptyState({ title, text, actionLabel, actionTo }) {
  return (
    <div className="cf-empty">
      <h3>{title}</h3>
      {text && <p className="cf-muted">{text}</p>}
      {actionLabel && <Link className="cf-btn cf-btn-primary" to={actionTo}>{actionLabel}</Link>}
    </div>
  );
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="cf-page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="cf-muted">{subtitle}</p>}
      </div>
      <div>{children}</div>
    </div>
  );
}

export function StatCard({ label, value, hint }) {
  return (
    <div className="cf-card cf-stat">
      <span className="cf-muted">{label}</span>
      <strong>{value}</strong>
      {hint && <small className="cf-muted">{hint}</small>}
    </div>
  );
}

export function Button({ variant = 'primary', loading, children, ...props }) {
  return (
    <button className={`cf-btn cf-btn-${variant}`} disabled={loading || props.disabled} {...props}>
      {loading ? 'Please wait...' : children}
    </button>
  );
}

export function Field({ label, id, error, children }) {
  return (
    <div className="cf-field">
      <label htmlFor={id}>{label}</label>
      {children}
      {error && <small className="cf-error-text">{error}</small>}
    </div>
  );
}

export function Badge({ tone = 'neutral', children }) {
  return <span className={`cf-badge cf-badge-${tone}`}>{children}</span>;
}
