import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errorMessage } from '../../api/client.js';
import { Badge, EmptyState, ErrorMessage, LoadingSpinner, PageHeader, StatCard } from '../../components/ui.jsx';
import { formatDate, formatPercent } from '../../utils/format.js';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/dashboard/summary').then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <ErrorMessage message={error} />;
  if (!data) return <LoadingSpinner />;

  const noActivity = data.testsCompleted === 0;

  return (
    <>
      <PageHeader title={`Welcome, ${data.fullName}`}
        subtitle={data.targetCareer ? `Target career: ${data.targetCareer}` : 'You have not chosen a target career yet.'}>
        <Link className="cf-btn cf-btn-primary" to="/tests">Take a test</Link>
      </PageHeader>

      {!data.targetCareer && (
        <div className="cf-alert cf-alert-warn">
          Pick a target career to get tailored recommendations. <Link to="/career-paths">Browse career paths</Link>
        </div>
      )}

      <div className="cf-grid-stats">
        <StatCard label="Tests completed" value={data.testsCompleted} />
        <StatCard label="Average score" value={formatPercent(data.averageScore)} />
        <StatCard label="Best score" value={formatPercent(data.bestScore)} />
      </div>

      {noActivity ? (
        <EmptyState title="No test activity yet"
          text="Complete a mock test and your scores, skill performance and recommendations will appear here."
          actionLabel="Browse tests" actionTo="/tests" />
      ) : (
        <div className="cf-two-col cf-section">
          <div className="cf-card">
            <h2>Skill-wise accuracy</h2>
            <div style={{ width: '100%', height: 260 }}>
              <ResponsiveContainer>
                <BarChart data={data.skillPerformance}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e1e4ee" />
                  <XAxis dataKey="skill" tick={{ fontSize: 12 }} />
                  <YAxis domain={[0, 100]} unit="%" />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Bar dataKey="accuracy" fill="#27348b" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="cf-card">
            <h2>Recent attempts</h2>
            <div className="cf-table-wrap">
              <table className="cf-table">
                <thead><tr><th>Test</th><th>Score</th><th>Date</th></tr></thead>
                <tbody>
                  {data.recentAttempts.map((a) => (
                    <tr key={a.id}>
                      <td><Link to={`/results/${a.id}`}>{a.testTitle}</Link></td>
                      <td>{formatPercent(a.percentage)}</td>
                      <td>{formatDate(a.startedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      <div className="cf-card">
        <h2>Recommended next steps</h2>
        {data.recommendedTopics.length === 0 ? (
          <p className="cf-muted">Recommendations appear after you take tests or choose a target career.</p>
        ) : (
          <div className="cf-pill-list">
            {data.recommendedTopics.map((t) => <Badge key={t} tone="warning">{t}</Badge>)}
          </div>
        )}
      </div>
    </>
  );
}
