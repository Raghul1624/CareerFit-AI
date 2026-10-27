import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, Button, EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { formatDate, formatDuration, formatPercent } from '../../utils/format.js';
import { modeLabel } from '../../utils/mockTests.js';

const statusLabel = { IN_PROGRESS: 'In progress', SUBMITTED: 'Submitted', AUTO_SUBMITTED: 'Auto-submitted' };

export default function TestHistory() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState('date');
  const [direction, setDirection] = useState('desc');
  const [error, setError] = useState('');

  useEffect(() => {
    setData(null);
    api.get('/attempts/history', { params: { page, size: 10, sort, direction } })
      .then((r) => setData(r.data))
      .catch((e) => setError(errorMessage(e)));
  }, [page, sort, direction]);

  return (
    <>
      <PageHeader title="Test history" subtitle="All of your previous attempts.">
        <div style={{ display: 'flex', gap: 8 }}>
          <select className="cf-input" value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }} aria-label="Sort by">
            <option value="date">Sort by date</option>
            <option value="score">Sort by score</option>
            <option value="title">Sort by test name</option>
          </select>
          <select className="cf-input" value={direction} onChange={(e) => { setDirection(e.target.value); setPage(0); }} aria-label="Direction">
            <option value="desc">Descending</option>
            <option value="asc">Ascending</option>
          </select>
        </div>
      </PageHeader>
      <ErrorMessage message={error} />
      {!data && !error && <LoadingSpinner />}
      {data && data.content.length === 0 && (
        <EmptyState title="No attempts yet" text="Your finished tests will be listed here." actionLabel="Browse tests" actionTo="/tests" />
      )}
      {data && data.content.length > 0 && (
        <div className="cf-card">
          <div className="cf-table-wrap">
            <table className="cf-table">
              <thead><tr><th>Test</th><th>Type</th><th>Date</th><th>Score</th><th>Percentage</th><th>Time taken</th><th>Status</th><th /></tr></thead>
              <tbody>
                {data.content.map((a) => (
                  <tr key={a.id}>
                    <td>{a.testTitle}</td>
                    <td>
                      <Badge tone={a.mode === 'CAREER' ? 'success' : 'neutral'}>{modeLabel(a.mode)}</Badge>
                      {a.careerPathName && <div className="cf-muted"><small>{a.careerPathName}</small></div>}
                    </td>
                    <td>{formatDate(a.startedAt)}</td>
                    <td>{a.score == null ? '-' : `${a.score} / ${a.maxScore}`}</td>
                    <td>{formatPercent(a.percentage)}</td>
                    <td>{a.timeTakenSeconds == null ? '-' : formatDuration(a.timeTakenSeconds)}</td>
                    <td><Badge tone={a.status === 'IN_PROGRESS' ? 'warning' : 'neutral'}>{statusLabel[a.status]}</Badge></td>
                    <td>
                      {a.status === 'IN_PROGRESS'
                        ? <Link className="cf-btn cf-btn-outline cf-btn-sm" to={`/attempts/${a.id}/take`}>Resume</Link>
                        : <Link className="cf-btn cf-btn-outline cf-btn-sm" to={`/results/${a.id}`}>View results</Link>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 12, alignItems: 'center' }}>
            <span className="cf-muted">Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
            <Button variant="outline" onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</Button>
            <Button variant="outline" onClick={() => setPage(page + 1)} disabled={page + 1 >= data.totalPages}>Next</Button>
          </div>
        </div>
      )}
    </>
  );
}
