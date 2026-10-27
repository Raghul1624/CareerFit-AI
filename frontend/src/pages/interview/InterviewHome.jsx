import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { getOverview, listRoles } from '../../api/interview.js';
import { Badge, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { formatDate, formatPercent } from '../../utils/format.js';
import { interviewAttemptPath, interviewResultPath, levelLabel, roundAvailable, roundShortLabel } from '../../utils/interview.js';

export default function InterviewHome() {
  const [roles, setRoles] = useState(null);
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([listRoles(), getOverview()])
      .then(([r, o]) => { setRoles(r); setOverview(o); })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <PageHeader title="Interview Preparation" subtitle="Practise the five rounds of a real hiring process for your target role." />
      <div className="cf-steps">
        {['Choose a role', 'Pick a round', 'Select a difficulty', 'Get scored feedback'].map((s, i) => (
          <div key={s} className="cf-step"><span>{i + 1}</span>{s}</div>
        ))}
      </div>
      <ErrorMessage message={error} />
      {!roles && !error && <LoadingSpinner />}
      {roles && (
        <div className="cf-grid">
          {roles.map((role) => {
            const ready = role.rounds.filter(roundAvailable).length;
            return (
              <div key={role.code} className="cf-card cf-role-card">
                <h3>{role.name}</h3>
                <p className="cf-muted">{role.description}</p>
                <div className="cf-pill-list">
                  {role.rounds.map((r) => <Badge key={r.round} tone={roundAvailable(r) ? 'neutral' : 'warning'}>{roundShortLabel(r.round)}</Badge>)}
                </div>
                <Link className="cf-btn cf-btn-primary cf-btn-sm" to={`/interview/${role.code}`}>
                  {ready > 0 ? 'Choose this role' : 'View rounds'}
                </Link>
              </div>
            );
          })}
        </div>
      )}

      {overview && overview.recent.length > 0 && (
        <section className="cf-section">
          <div className="cf-section-head">
            <h2>Recent attempts</h2>
            <Link to="/interview/history">View all history</Link>
          </div>
          <div className="cf-card">
            <div className="cf-table-wrap">
              <table className="cf-table">
                <thead><tr><th>Role</th><th>Round</th><th>Level</th><th>Date</th><th>Score</th><th /></tr></thead>
                <tbody>
                  {overview.recent.map((a) => (
                    <tr key={a.id}>
                      <td>{a.roleName}</td><td>{roundShortLabel(a.round)}</td><td>{levelLabel(a.level)}</td><td>{formatDate(a.startedAt)}</td>
                      <td>{a.status === 'IN_PROGRESS' ? <Badge tone="warning">In progress</Badge> : formatPercent(a.percentage)}</td>
                      <td>
                        {a.status === 'IN_PROGRESS'
                          ? <Link className="cf-btn cf-btn-outline cf-btn-sm" to={interviewAttemptPath(a.id)}>Resume</Link>
                          : <Link className="cf-btn cf-btn-outline cf-btn-sm" to={interviewResultPath(a.id)}>View result</Link>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}
    </>
  );
}
