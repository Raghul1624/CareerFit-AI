import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { listCareerPaths, listMockTests, listSkills } from '../../api/mockTests.js';
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import TestCard from '../../components/mocktest/TestCard.jsx';
import TestFilters from '../../components/mocktest/TestFilters.jsx';
import { matchesSearch } from '../../utils/mockTests.js';

/**
 * Mock test catalogue. Two kinds of test are kept apart:
 *  - Career Mock Tests combine the topics one career path needs (for example Java, OOP, SQL and Spring Boot).
 *  - Skill Tests cover a single skill only.
 * The filters live in the URL (?mode=&careerPathId=&skillId=&q=), so a link from a career page or a
 * browser back button always lands on the same selection.
 */
export default function MockTests() {
  const [params, setParams] = useSearchParams();
  const values = {
    mode: params.get('mode') || '',
    careerPathId: params.get('careerPathId') || '',
    skillId: params.get('skillId') || '',
    q: params.get('q') || '',
  };

  const [tests, setTests] = useState(null);
  const [careerPaths, setCareerPaths] = useState([]);
  const [skills, setSkills] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([listCareerPaths(), listSkills()])
      .then(([paths, allSkills]) => { setCareerPaths(paths); setSkills(allSkills); })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(() => {
    let cancelled = false;
    setTests(null);
    const query = {};
    if (values.mode) query.mode = values.mode;
    if (values.careerPathId) query.careerPathId = values.careerPathId;
    if (values.skillId) query.skillId = values.skillId;
    listMockTests(query)
      .then((data) => { if (!cancelled) { setTests(data); setError(''); } })
      .catch((e) => { if (!cancelled) setError(errorMessage(e)); });
    return () => { cancelled = true; };
  }, [values.mode, values.careerPathId, values.skillId]);

  const onChange = (next) => {
    const merged = { ...next };
    if (merged.mode === 'SKILL') merged.careerPathId = '';   // a skill test never belongs to a career path
    const out = new URLSearchParams();
    Object.entries(merged).forEach(([key, value]) => { if (value) out.set(key, value); });
    setParams(out, { replace: true });
  };

  const visible = useMemo(() => (tests || []).filter((t) => matchesSearch(t, values.q)), [tests, values.q]);

  return (
    <>
      <PageHeader title="Mock tests"
        subtitle="Take a career mock test that combines everything a role needs, or practise one skill at a time." />
      <TestFilters values={values} careerPaths={careerPaths} skills={skills} onChange={onChange} />
      <ErrorMessage message={error} />
      {!tests && !error && <LoadingSpinner />}
      {tests && visible.length === 0 && (
        <EmptyState title="No tests found" text="Try a different type, career path, skill or search word." />
      )}
      {visible.length > 0 && (
        <>
          <p className="cf-muted" style={{ margin: '12px 0' }}>{visible.length} test{visible.length === 1 ? '' : 's'}</p>
          <div className="cf-grid cf-test-grid">
            {visible.map((t) => <TestCard key={t.id} test={t} />)}
          </div>
        </>
      )}
    </>
  );
}
