import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { getHistory, listRoles } from '../../api/interview.js';
import { Badge, Button, EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { formatDate, formatPercent } from '../../utils/format.js';
import { interviewAttemptPath, interviewResultPath, levelLabel, levelTone, ROUND_ORDER, roundShortLabel } from '../../utils/interview.js';

const statusLabel = { IN_PROGRESS: 'In progress', SUBMITTED: 'Submitted', AUTO_SUBMITTED: 'Auto-submitted' };

export default function InterviewHistory() {
  const [data, setData] = useState(null);
  const [roles, setRoles] = useState([]);
  const [page, setPage] = useState(0);
  const [round, setRound] = useState('');
  const [role, setRole] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { listRoles().then(setRoles).catch(() => setRoles([])); }, []);
  useEffect(() => {
    setData(null);
    const params = { page, size: 10 };
    if (round) params.round = round;
    if (role) params.role = role;
    getHistory(params).then(setData).catch((e) => setError(errorMessage(e)));
  }, [page, round, role]);

  return (
    <>
      <PageHeader title="Interview attempt history" subtitle="Every interview-preparation round you started.">
        <div className="cf-actions-row" style={{ margin: 0 }}>
          <select className="cf-input" aria-label="Filter by role" value={role} onChange={(e) => { setRole(e.target.value); setPage(0); }}>
            <option value="">All roles</option>
            {roles.map((r) => <option key={r.code} value={r.code}>{r.name}</option>)}
          </select>
          <select className="cf-input" aria-label="Filter by round" value={round} onChange={(e) => { setRound(e.target.value); setPage(0); }}>
            <option value="">All rounds</option>
            {ROUND_ORDER.map((r) => <option key={r} value={r}>{roundShortLabel(r)}</option>)}
          </select>
        </div>
      </PageHeader>
      <ErrorMessage message={error} />
      {!data && !error && <LoadingSpinner />}
      {data && data.content.length === 0 && (
        <EmptyState title="No attempts yet" text="Your interview-preparation attempts will be listed here." actionLabel="Start preparing" actionTo="/interview" />
      )}
      {data && data.content.length > 0 && (
        <div className="cf-card">
          <div className="cf-table-wrap">
            <table className="cf-table">
              <thead><tr><th>Role</th><th>Round</th><th>Level</th><th>Date</th><th>Score</th><th>Result</th><th>Status</th><th /></tr></thead>
              <tbody>
                {data.content.map((a) => (
                  <tr key={a.id}>
                    <td>{a.roleName}</td>
                    <td>{roundShortLabel(a.round)}</td>
                    <td><Badge tone={levelTone(a.level)}>{levelLabel(a.level)}</Badge></td>
                    <td>{formatDate(a.startedAt)}</td>
                    <td>{a.percentage == null ? '-' : formatPercent(a.percentage)}</td>
                    <td>{a.passed == null ? '-' : <Badge tone={a.passed ? 'success' : 'danger'}>{a.passed ? 'Passed' : 'Not passed'}</Badge>}</td>
                    <td><Badge tone={a.status === 'IN_PROGRESS' ? 'warning' : 'neutral'}>{statusLabel[a.status]}</Badge></td>
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
          <div className="cf-pager">
            <span className="cf-muted">Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
            <Button variant="outline" onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</Button>
            <Button variant="outline" onClick={() => setPage(page + 1)} disabled={page + 1 >= data.totalPages}>Next</Button>
          </div>
        </div>
      )}
    </>
  );
}
