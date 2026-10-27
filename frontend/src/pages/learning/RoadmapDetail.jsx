import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';

export default function RoadmapDetail() {
  const { id } = useParams();
  const [roadmap, setRoadmap] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/learning-roadmaps/${id}`).then((r) => setRoadmap(r.data)).catch((e) => setError(errorMessage(e)));
  }, [id]);

  const toggle = async (item) => {
    setError('');
    try {
      const { data } = await api.put(`/learning-progress/${item.id}`, { completed: !item.completed });
      setRoadmap((r) => {
        const items = r.items.map((i) => (i.id === item.id ? { ...i, completed: data.completed, completedAt: data.completedAt } : i));
        const completedSteps = items.filter((i) => i.completed).length;
        return { ...r, items, completedSteps, progressPercent: Math.round((completedSteps * 1000) / items.length) / 10 };
      });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  if (error && !roadmap) return <ErrorMessage message={error} />;
  if (!roadmap) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title={roadmap.title} subtitle={`${roadmap.completedSteps} of ${roadmap.totalSteps} steps complete (${roadmap.progressPercent}%)`}>
        <Link className="cf-btn cf-btn-outline" to={`/career-paths/${roadmap.careerPathId}`}>View career path</Link>
      </PageHeader>
      <div className="cf-progress" style={{ marginBottom: 16 }}><div style={{ width: `${roadmap.progressPercent}%` }} /></div>
      <ErrorMessage message={error} />
      {roadmap.items.map((item) => (
        <div className="cf-card" key={item.id} style={{ marginBottom: 12, opacity: item.completed ? 0.8 : 1 }}>
          <label style={{ display: 'flex', gap: 10, alignItems: 'flex-start', cursor: 'pointer' }}>
            <input type="checkbox" checked={item.completed} onChange={() => toggle(item)} style={{ marginTop: 6 }} />
            <div>
              <strong>{item.stepOrder}. {item.title}</strong>
              {item.estimatedDuration && <span className="cf-muted"> &middot; {item.estimatedDuration}</span>}
              <p className="cf-muted" style={{ margin: '4px 0' }}>{item.description}</p>
            </div>
          </label>
          <div className="cf-pill-list" style={{ marginLeft: 26 }}>
            {item.skills.map((s) => <Badge key={s}>{s}</Badge>)}
            {item.recommendedTests.map((t) => <Link key={t.id} className="cf-btn cf-btn-outline cf-btn-sm" to={`/tests/${t.id}`}>Practice: {t.title}</Link>)}
          </div>
        </div>
      ))}
    </>
  );
}
