import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, Button, ErrorMessage, LoadingSpinner, PageHeader, SuccessMessage } from '../../components/ui.jsx';
import TestCard from '../../components/mocktest/TestCard.jsx';
import JobCard from '../jobs/JobCard.jsx';
import { difficultyLabel, testHref, testListHref } from '../../utils/mockTests.js';

export default function CareerPathDetails() {
  const { id } = useParams();
  const [path, setPath] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setPath(null);
    setJobs([]);
    api.get(`/career-paths/${id}`).then((r) => setPath(r.data)).catch((e) => setError(errorMessage(e)));
    // Jobs are optional context: a failure here must not hide the rest of the page.
    api.get('/jobs', { params: { careerPathId: id, size: 3 } })
      .then((r) => setJobs(r.data.content || []))
      .catch(() => setJobs([]));
  }, [id]);

  const setTarget = async () => {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      await api.put('/profile/target-career', { careerPathId: Number(id) });
      setMessage(`${path.name} is now your target career.`);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (error && !path) return <ErrorMessage message={error} />;
  if (!path) return <LoadingSpinner />;

  const test = path.careerMockTest;
  const skillTests = path.recommendedTests.filter((t) => t.mode === 'SKILL');

  return (
    <>
      <PageHeader title={path.name} subtitle={path.categoryName}>
        <Button onClick={setTarget} loading={busy}>Set as my target career</Button>
      </PageHeader>
      <ErrorMessage message={error} />
      <SuccessMessage message={message} />

      <section className="cf-section">
        <h2>Overview</h2>
        <p>{path.description}</p>
      </section>

      {/* The call to action: start the mock test built for this exact career path. */}
      <section className="cf-section">
        <div className="cf-card cf-cta">
          {test ? (
            <>
              <div>
                <Badge tone="success">Career mock test</Badge>
                <h2 style={{ margin: '6px 0' }}>{test.title}</h2>
                <p className="cf-muted">{test.description}</p>
                <p className="cf-muted">
                  {test.questionCount} questions &middot; {test.durationMinutes} min &middot; {difficultyLabel(test.difficulties)}
                  {' '}&middot; drawn from {test.availableQuestions} {path.name} questions
                </p>
              </div>
              {test.startable
                ? <Link className="cf-btn cf-btn-primary" to={testHref(test)}>Start {path.name} mock test</Link>
                : <span className="cf-muted">{test.unavailableReason}</span>}
            </>
          ) : (
            <p className="cf-muted">A mock test for this career path is not available yet.</p>
          )}
        </div>
      </section>

      <section className="cf-section">
        <h2>Required skills</h2>
        {path.skills.length === 0
          ? <p className="cf-muted">No skills have been configured for this role yet.</p>
          : <div className="cf-pill-list">{path.skills.map((s) => <Badge key={s.id}>{s.name}</Badge>)}</div>}
      </section>

      <section className="cf-section">
        <h2>Recommended learning topics</h2>
        {path.learningTopics.length === 0
          ? <p className="cf-muted">No learning topics are available yet.</p>
          : (
            <div className="cf-grid">
              {path.learningTopics.map((c) => (
                <div className="cf-card" key={c.skillId}>
                  <h3>{c.skillName}</h3>
                  <div className="cf-pill-list">{c.topics.map((t) => <Badge key={t}>{t}</Badge>)}</div>
                  <p className="cf-muted" style={{ marginTop: 8 }}>{c.questionCount} practice questions</p>
                  <Link to={testListHref({ mode: 'SKILL', skillId: c.skillId })}>Practise this skill</Link>
                </div>
              ))}
            </div>
          )}
      </section>

      <section className="cf-section">
        <h2>Recommended roadmap</h2>
        {path.roadmap ? (
          <div className="cf-card">
            <strong>{path.roadmap.title}</strong>
            <p className="cf-muted">{path.roadmap.steps} steps from the basics to a portfolio project.</p>
            <Link className="cf-btn cf-btn-outline cf-btn-sm" to={`/roadmap/${path.roadmap.id}`}>Open roadmap</Link>
          </div>
        ) : (
          <div className="cf-card">
            <p className="cf-muted">A detailed roadmap for this path is not available yet. A sensible order to learn the skills, foundations first:</p>
            <ol>{path.suggestedLearningOrder.map((name) => <li key={name}>{name}</li>)}</ol>
          </div>
        )}
      </section>

      <section className="cf-section">
        <h2>Skill tests for this role</h2>
        {skillTests.length === 0
          ? <p className="cf-muted">No skill tests are available for this role yet.</p>
          : <div className="cf-grid cf-test-grid">{skillTests.map((t) => <TestCard key={t.id} test={t} />)}</div>}
      </section>

      <section className="cf-section">
        <h2>Job opportunities</h2>
        {jobs.length === 0
          ? <p className="cf-muted">No listings match this career path right now.</p>
          : (
            <>
              <div className="cf-grid">{jobs.map((j) => <JobCard key={j.id} job={j} base="/job-search" />)}</div>
              <p><Link to="/job-search">Browse all jobs</Link></p>
            </>
          )}
      </section>
    </>
  );
}
