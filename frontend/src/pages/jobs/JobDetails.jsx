import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, Button, ErrorMessage, LoadingSpinner, PageHeader, SuccessMessage } from '../../components/ui.jsx';
import { labelize } from '../../utils/format.js';

const STATUSES = ['APPLIED', 'INTERVIEWING', 'OFFER', 'REJECTED', 'WITHDRAWN'];

export default function JobDetails({ publicMode = false }) {
  const { id } = useParams();
  const [job, setJob] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [app, setApp] = useState({ status: 'APPLIED', notes: '' });

  useEffect(() => {
    api.get(publicMode ? `/public/jobs/${id}` : `/jobs/${id}`)
      .then((r) => {
        setJob(r.data);
        if (r.data.application) setApp({ status: r.data.application.status, notes: r.data.application.notes || '' });
      })
      .catch((e) => setError(errorMessage(e)));
  }, [id, publicMode]);

  const run = async (fn, okMessage) => {
    setError('');
    setMessage('');
    try { await fn(); setMessage(okMessage); } catch (e) { setError(errorMessage(e)); }
  };

  const toggleSave = () => run(async () => {
    if (job.saved) await api.delete(`/jobs/${id}/save`); else await api.post(`/jobs/${id}/save`);
    setJob({ ...job, saved: !job.saved });
  }, job?.saved ? 'Removed from saved jobs.' : 'Job saved.');

  const saveApp = () => run(async () => {
    const { data } = await api.put(`/jobs/${id}/application`, { status: app.status, notes: app.notes });
    setJob({ ...job, application: data });
  }, 'Application status saved.');

  const removeApp = () => run(async () => {
    await api.delete(`/jobs/${id}/application`);
    setJob({ ...job, application: null });
    setApp({ status: 'APPLIED', notes: '' });
  }, 'Application tracking removed.');

  if (error && !job) return <div className={publicMode ? 'cf-content' : undefined}><ErrorMessage message={error} /></div>;
  if (!job) return <LoadingSpinner />;
  const m = job.match;

  return (
    <div className={publicMode ? 'cf-content' : undefined}>
      <PageHeader title={job.title} subtitle={`${job.company} · ${job.location || 'Location not specified'}`}>
        {!publicMode && <Button variant="outline" onClick={toggleSave}>{job.saved ? 'Unsave' : 'Save job'}</Button>}
      </PageHeader>
      <ErrorMessage message={error} />
      <SuccessMessage message={message} />
      <div className="cf-pill-list" style={{ marginBottom: 14 }}>
        {job.sample && <Badge tone="warning">Sample listing, not a live vacancy</Badge>}
        {job.workMode && <Badge>{labelize(job.workMode)}</Badge>}
        {job.experienceLevel && <Badge>{labelize(job.experienceLevel)}</Badge>}
        {job.employmentType && <Badge>{labelize(job.employmentType)}</Badge>}
      </div>

      <div className="cf-two-col">
        <div className="cf-card">
          <h2>About this role</h2>
          <p style={{ whiteSpace: 'pre-wrap' }}>{job.description || 'No description provided.'}</p>
          <h3>Required skills</h3>
          <div className="cf-pill-list">{job.requiredSkills.map((s) => (
            <Badge key={s} tone={m ? (m.missingRequired.includes(s) ? 'danger' : 'success') : 'neutral'}>{s}</Badge>))}</div>
          {job.preferredSkills.length > 0 && <>
            <h3 style={{ marginTop: 14 }}>Preferred skills</h3>
            <div className="cf-pill-list">{job.preferredSkills.map((s) => (
              <Badge key={s} tone={m ? (m.missingPreferred.includes(s) ? 'warning' : 'success') : 'neutral'}>{s}</Badge>))}</div>
          </>}
          {job.applicationUrl && (
            <p style={{ marginTop: 16 }}>
              <a className="cf-btn cf-btn-primary" href={job.applicationUrl} target="_blank" rel="noopener noreferrer">Apply on the company site</a>
            </p>
          )}
        </div>

        {publicMode ? (
          <div className="cf-card">
            <h2>Want to see how you match?</h2>
            <p className="cf-muted">Log in to see a skill match score, save jobs and track applications.</p>
            <Link className="cf-btn cf-btn-primary" to="/login">Log in</Link>
          </div>
        ) : (
          <div>
            <div className="cf-card" style={{ marginBottom: 16 }}>
              <h2>Your match: {m.percent}%</h2>
              <div className="cf-progress"><div style={{ width: `${m.percent}%` }} /></div>
              <p className="cf-muted" style={{ marginTop: 8 }}>
                Required skills count 70%, preferred 30%. Skills count if you declared them or scored 70%+ in tests.
                {m.missingRequired.length > 0 && <> Missing required: <strong>{m.missingRequired.join(', ')}</strong>.</>}
              </p>
              <Link to="/profile">Update my skills</Link>
            </div>
            <div className="cf-card">
              <h2>Application tracker</h2>
              <div className="cf-field">
                <label htmlFor="status">Status</label>
                <select id="status" className="cf-input" value={app.status} onChange={(e) => setApp({ ...app, status: e.target.value })}>
                  {STATUSES.map((s) => <option key={s} value={s}>{labelize(s)}</option>)}
                </select>
              </div>
              <div className="cf-field">
                <label htmlFor="notes">Notes</label>
                <textarea id="notes" className="cf-input" rows={3} maxLength={500} value={app.notes}
                  onChange={(e) => setApp({ ...app, notes: e.target.value })} />
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <Button onClick={saveApp}>{job.application ? 'Update' : 'Track application'}</Button>
                {job.application && <Button variant="outline" onClick={removeApp}>Remove</Button>}
              </div>
              <p className="cf-muted" style={{ marginTop: 8, fontSize: '0.8rem' }}>This only records your own progress; it does not apply on your behalf.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
