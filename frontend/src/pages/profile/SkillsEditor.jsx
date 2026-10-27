import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { Button, ErrorMessage, SuccessMessage } from '../../components/ui.jsx';

export default function SkillsEditor() {
  const [all, setAll] = useState([]);
  const [selected, setSelected] = useState(new Set());
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([api.get('/skills'), api.get('/profile/skills')])
      .then(([a, m]) => { setAll(a.data); setSelected(new Set(m.data.map((s) => s.id))); })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const toggle = (id) => {
    const next = new Set(selected);
    next.has(id) ? next.delete(id) : next.add(id);
    setSelected(next);
    setMessage('');
  };

  const save = async () => {
    setBusy(true);
    setError('');
    try {
      await api.put('/profile/skills', { skillIds: [...selected] });
      setMessage('Skills saved.');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="cf-card" style={{ maxWidth: 560, marginTop: 16 }}>
      <h2>Your skills</h2>
      <p className="cf-muted">Select the skills you already have. They are used in your skill-gap analysis and stay unverified until tests confirm them.</p>
      <ErrorMessage message={error} />
      <SuccessMessage message={message} />
      <div className="cf-pill-list" style={{ marginBottom: 14 }}>
        {all.map((s) => (
          <label key={s.id} className={`cf-option ${selected.has(s.id) ? 'selected' : ''}`} style={{ margin: 0, padding: '6px 10px' }}>
            <input type="checkbox" checked={selected.has(s.id)} onChange={() => toggle(s.id)} />
            <span>{s.name}</span>
          </label>
        ))}
      </div>
      <Button onClick={save} loading={busy}>Save skills</Button>
    </div>
  );
}
