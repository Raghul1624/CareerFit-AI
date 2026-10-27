import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { errorMessage } from '../../api/client.js';
import { Button, ErrorMessage, Field } from '../../components/ui.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(form.password)) {
      setError('Password must be 8-72 characters and contain a letter and a number.');
      return;
    }
    setBusy(true);
    try {
      await register(form.fullName, form.email, form.password);
      navigate('/career-paths', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="cf-card cf-auth-card">
      <h1>Create your account</h1>
      <p className="cf-muted">Start practising in a few minutes.</p>
      <ErrorMessage message={error} />
      <form onSubmit={submit}>
        <Field label="Full name" id="fullName">
          <input id="fullName" className="cf-input" required maxLength={100} value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="Email" id="email">
          <input id="email" className="cf-input" type="email" required value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} autoComplete="email" />
        </Field>
        <Field label="Password" id="password">
          <input id="password" className="cf-input" type="password" required value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} autoComplete="new-password" />
        </Field>
        <Button type="submit" loading={busy}>Create account</Button>
      </form>
      <p className="cf-muted" style={{ marginTop: 14 }}>Already registered? <Link to="/login">Log in</Link></p>
    </div>
  );
}
