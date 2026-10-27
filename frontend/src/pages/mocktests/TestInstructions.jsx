import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { getMockTest, startAttempt } from '../../api/mockTests.js';
import { Badge, Button, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { backLink, difficultyLabel, modeLabel } from '../../utils/mockTests.js';

/** Test details and instructions, shown before the clock starts. */
export default function TestInstructions() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [test, setTest] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setTest(null);
    getMockTest(id).then(setTest).catch((e) => setError(errorMessage(e)));
  }, [id]);

  const start = async () => {
    setBusy(true);
    setError('');
    try {
      const attempt = await startAttempt(id);
      navigate(`/attempts/${attempt.id}/take`);
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  };

  if (error && !test) return <ErrorMessage message={error} />;
  if (!test) return <LoadingSpinner />;

  const back = backLink(test);
  const isCareer = test.mode === 'CAREER';

  return (
    <>
      <p><Link to={back.to}>&larr; {back.label}</Link></p>
      <PageHeader title={test.title} subtitle={test.description}>
        <Badge tone={isCareer ? 'success' : 'neutral'}>{modeLabel(test.mode)}</Badge>
      </PageHeader>
      <ErrorMessage message={error} />

      <div className="cf-two-col">
        <div className="cf-card">
          <h2>About this test</h2>
          <dl className="cf-details">
            <dt>{isCareer ? 'Career path' : 'Skill'}</dt>
            <dd>{isCareer ? <Link to={`/career-paths/${test.careerPathId}`}>{test.careerPathName}</Link> : test.skills[0]}</dd>
            <dt>Questions</dt><dd>{test.questionCount} (drawn at random from {test.availableQuestions})</dd>
            <dt>Duration</dt><dd>{test.durationMinutes} minutes</dd>
            <dt>Difficulty</dt><dd>{difficultyLabel(test.difficulties)}</dd>
            <dt>Scoring</dt>
            <dd>
              {test.marksPerQuestion} mark per correct answer, total {test.totalMarks}.{' '}
              {test.negativeMarks > 0 ? `${test.negativeMarks} mark deducted for each wrong answer.` : 'No negative marking.'}{' '}
              Unanswered questions score 0. Pass mark: {test.passingPercentage}%.
            </dd>
            {test.maxAttempts && (<><dt>Attempts</dt><dd>Up to {test.maxAttempts}</dd></>)}
          </dl>

          <h3>Topics covered</h3>
          {test.coverage.map((c) => (
            <div key={c.skillId} style={{ marginBottom: 10 }}>
              {isCareer && <strong>{c.skillName}</strong>}
              <div className="cf-pill-list" style={{ marginTop: 4 }}>
                {c.topics.map((t) => <Badge key={t}>{t}</Badge>)}
              </div>
            </div>
          ))}
        </div>

        <div className="cf-card">
          <h2>Instructions</h2>
          <ul className="cf-instructions">
            <li>The timer starts as soon as you press <strong>Start test</strong>. The server keeps the clock, so refreshing the page does not add time.</li>
            <li>Move between questions with Previous, Next or the question palette. You can change an answer at any time before submitting.</li>
            <li>Use <strong>Mark for review</strong> to flag questions you want to revisit.</li>
            <li>Questions marked &quot;select all that apply&quot; need every correct option selected to earn marks.</li>
            <li>Answers are saved automatically. The test is submitted for you when time runs out.</li>
            <li>Correct answers and explanations appear after you submit, never during the test.</li>
            <li>Each attempt uses a fresh random selection, and questions you have not seen yet are preferred on retakes.</li>
          </ul>
          {test.startable
            ? <Button onClick={start} loading={busy}>Start test</Button>
            : <div className="cf-alert cf-alert-warn" role="alert">{test.unavailableReason}</div>}
        </div>
      </div>
    </>
  );
}
