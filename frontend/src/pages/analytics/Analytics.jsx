import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errorMessage } from '../../api/client.js';
import { Badge, EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';

const tone = { STRONG: 'success', IMPROVE: 'warning', UNVERIFIED: 'neutral', MISSING: 'danger' };
const label = { STRONG: 'Strong', IMPROVE: 'Needs improvement', UNVERIFIED: 'Not yet verified', MISSING: 'Missing' };

export default function Analytics() {
  const [perf, setPerf] = useState(null);
  const [gap, setGap] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.get('/analytics/performance'), api.get('/analytics/skill-gap')])
      .then(([p, g]) => { setPerf(p.data); setGap(g.data); })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <ErrorMessage message={error} />;
  if (!perf || !gap) return <LoadingSpinner />;

  const trend = perf.trend.map((t, i) => ({ n: i + 1, percentage: t.percentage, title: t.testTitle }));

  return (
    <>
      <PageHeader title="Performance analytics" subtitle="Calculated from your finished attempts and your target career." />

      {perf.trend.length === 0 ? (
        <EmptyState title="No finished tests yet" text="Complete a mock test to see your score trend and skill accuracy."
          actionLabel="Browse tests" actionTo="/tests" />
      ) : (
        <div className="cf-two-col cf-section">
          <div className="cf-card">
            <h2>Score trend (last {trend.length} attempts)</h2>
            <div style={{ width: '100%', height: 250 }}>
              <ResponsiveContainer>
                <LineChart data={trend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e1e4ee" />
                  <XAxis dataKey="n" label={{ value: 'Attempt', position: 'insideBottom', offset: -2 }} />
                  <YAxis domain={[0, 100]} unit="%" />
                  <Tooltip formatter={(v) => `${v}%`} labelFormatter={(n) => trend[n - 1]?.title} />
                  <Line type="monotone" dataKey="percentage" stroke="#0d9488" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="cf-card">
            <h2>Accuracy by skill</h2>
            <div style={{ width: '100%', height: 250 }}>
              <ResponsiveContainer>
                <BarChart data={perf.skills}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e1e4ee" />
                  <XAxis dataKey="skill" tick={{ fontSize: 12 }} />
                  <YAxis domain={[0, 100]} unit="%" />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Bar dataKey="accuracy" fill="#27348b" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      <div className="cf-card">
        <h2>Skill gap{gap.targetCareer ? ` for ${gap.targetCareer}` : ''}</h2>
        {!gap.targetCareer ? (
          <p className="cf-muted">Choose a target career to compare your skills against its requirements. <Link to="/career-paths">Browse career paths</Link></p>
        ) : (
          <>
            <p className="cf-muted">Strong means 70%+ accuracy in tests. Skills you list on your <Link to="/profile">profile</Link> stay "not yet verified" until a test confirms them.</p>
            <div className="cf-table-wrap">
              <table className="cf-table">
                <thead><tr><th>Skill</th><th>Status</th><th>Why</th><th>Practice</th></tr></thead>
                <tbody>
                  {gap.items.map((g) => (
                    <tr key={g.skillId}>
                      <td>{g.skill}</td>
                      <td><Badge tone={tone[g.status]}>{label[g.status]}</Badge></td>
                      <td className="cf-muted">{g.reason}</td>
                      <td>{g.recommendedTests.length === 0 ? <span className="cf-muted">-</span>
                        : g.recommendedTests.map((t) => <div key={t.id}><Link to={`/tests/${t.id}`}>{t.title}</Link></div>)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {gap.suggestedOrder.length > 0 && (
              <p style={{ marginTop: 12 }}><strong>Suggested learning order:</strong> {gap.suggestedOrder.join(' → ')}</p>
            )}
          </>
        )}
      </div>
    </>
  );
}
