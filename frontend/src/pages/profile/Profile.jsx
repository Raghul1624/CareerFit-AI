import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import SkillsEditor from './SkillsEditor.jsx';
import { Button, ErrorMessage, Field, LoadingSpinner, PageHeader, SuccessMessage } from '../../components/ui.jsx';

export default function Profile() {
  const [form, setForm] = useState(null);
  const [paths, setPaths] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([api.get('/profile'), api.get('/career-paths')])
      .then(([p, c]) => {
        setForm({ ...p.data, headline: p.data.headline || '', education: p.data.education || '', targetCareerPathId: p.data.targetCareerPathId || '' });
        setPaths(c.data);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setBusy(true);
    try {
      await api.put('/profile', {
        fullName: form.fullName,
        headline: form.headline,
        education: form.education,
        targetCareerPathId: form.targetCareerPathId ? Number(form.targetCareerPathId) : null,
      });
      setMessage('Profile saved.');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (error && !form) return <ErrorMessage message={error} />;
  if (!form) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title="Your profile" subtitle={form.email} />
      <div className="cf-card" style={{ maxWidth: 560 }}>
        <ErrorMessage message={error} />
        <SuccessMessage message={message} />
        <form onSubmit={save}>
          <Field label="Full name" id="fullName">
            <input id="fullName" className="cf-input" required maxLength={100} value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          </Field>
          <Field label="Headline" id="headline">
            <input id="headline" className="cf-input" maxLength={150} value={form.headline}
              onChange={(e) => setForm({ ...form, headline: e.target.value })} />
          </Field>
          <Field label="Education" id="education">
            <input id="education" className="cf-input" maxLength={200} value={form.education}
              onChange={(e) => setForm({ ...form, education: e.target.value })} />
          </Field>
          <Field label="Target career" id="target">
            <select id="target" className="cf-input" value={form.targetCareerPathId}
              onChange={(e) => setForm({ ...form, targetCareerPathId: e.target.value })}>
              <option value="">Not selected</option>
              {paths.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </Field>
          <Button type="submit" loading={busy}>Save profile</Button>
        </form>
        <p className="cf-muted" style={{ marginTop: 14 }}>Password change and account settings arrive in a later phase.</p>
      </div>
      <SkillsEditor />
    </>
  );
}
