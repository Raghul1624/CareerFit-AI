import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { testListHref } from '../../utils/mockTests.js';

export default function CareerPaths() {
  const [paths, setPaths] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/career-paths').then((r) => setPaths(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  const grouped = useMemo(() => {
    const map = new Map();
    (paths || []).forEach((p) => {
      if (!map.has(p.categoryName)) map.set(p.categoryName, []);
      map.get(p.categoryName).push(p);
    });
    return [...map.entries()];
  }, [paths]);

  if (error) return <ErrorMessage message={error} />;
  if (!paths) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title="Career paths" subtitle="Explore roles, the skills they need and the tests to practise with." />
      {grouped.map(([category, items]) => (
        <section className="cf-section" key={category}>
          <h2>{category}</h2>
          <div className="cf-grid">
            {items.map((p) => (
              <div className="cf-card" key={p.id}>
                <h3>{p.name}</h3>
                <p className="cf-muted">{p.description}</p>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <Link className="cf-btn cf-btn-outline cf-btn-sm" to={`/career-paths/${p.id}`}>View details</Link>
                  <Link className="cf-btn cf-btn-primary cf-btn-sm"
                    to={testListHref({ mode: 'CAREER', careerPathId: p.id })}>Take mock test</Link>
                </div>
              </div>
            ))}
          </div>
        </section>
      ))}
    </>
  );
}
