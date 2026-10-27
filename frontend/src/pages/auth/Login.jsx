import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { errorMessage } from '../../api/client.js';
import { Button, ErrorMessage, Field } from '../../components/ui.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(form.email, form.password);
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="cf-card cf-auth-card">
      <h1>Log in</h1>
      <p className="cf-muted">Welcome back to CareerFit AI.</p>
      <ErrorMessage message={error} />
      <form onSubmit={submit}>
        <Field label="Email" id="email">
          <input id="email" className="cf-input" type="email" required value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} autoComplete="email" />
        </Field>
        <Field label="Password" id="password">
          <input id="password" className="cf-input" type="password" required value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} autoComplete="current-password" />
        </Field>
        <Button type="submit" loading={busy}>Log in</Button>
      </form>
      <p className="cf-muted" style={{ marginTop: 14 }}>New here? <Link to="/register">Create an account</Link></p>
    </div>
  );
}
