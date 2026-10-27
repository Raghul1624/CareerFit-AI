import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';

export default function LearningRoadmaps() {
  const [list, setList] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/learning-roadmaps').then((r) => setList(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <ErrorMessage message={error} />;
  if (!list) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title="Learning roadmaps" subtitle="Step-by-step plans. Mark steps complete to track your progress." />
      {list.length === 0 ? <EmptyState title="No roadmaps available yet" /> : (
        <div className="cf-grid">
          {list.map((r) => (
            <div className="cf-card" key={r.id}>
              <h3>{r.title}</h3>
              <p className="cf-muted">{r.description}</p>
              <div className="cf-progress"><div style={{ width: `${r.totalSteps ? (r.completedSteps * 100) / r.totalSteps : 0}%` }} /></div>
              <p className="cf-muted">{r.completedSteps} of {r.totalSteps} steps done</p>
              <Link className="cf-btn cf-btn-outline cf-btn-sm" to={`/roadmap/${r.id}`}>Open roadmap</Link>
            </div>
          ))}
        </div>
      )}
      <p className="cf-muted" style={{ marginTop: 16 }}>Roadmaps are sample content; more career paths will be added.</p>
    </>
  );
}
