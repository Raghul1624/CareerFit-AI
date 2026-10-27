import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, ErrorMessage, LoadingSpinner, PageHeader, StatCard } from '../../components/ui.jsx';
import { formatDuration, formatPercent } from '../../utils/format.js';
import { modeLabel, testListHref } from '../../utils/mockTests.js';

export default function TestResult() {
  const { id } = useParams();
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/attempts/${id}/result`).then((r) => setResult(r.data)).catch((e) => setError(errorMessage(e)));
  }, [id]);

  if (error) return <ErrorMessage message={error} />;
  if (!result) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title={result.testTitle}
        subtitle={result.status === 'AUTO_SUBMITTED' ? 'Submitted automatically when time ran out' : 'Test result'}>
        <Badge tone={result.passed ? 'success' : 'danger'}>{result.passed ? 'Passed' : 'Not passed'}</Badge>
      </PageHeader>
      <div className="cf-test-card-badges" style={{ marginBottom: 12 }}>
        <Badge tone={result.mode === 'CAREER' ? 'success' : 'neutral'}>{modeLabel(result.mode)}</Badge>
        {result.careerPathName && (
          <Link to={`/career-paths/${result.careerPathId}`}><Badge>{result.careerPathName}</Badge></Link>
        )}
      </div>

      <div className="cf-grid-stats">
        <StatCard label="Score" value={`${result.score} / ${result.maxScore}`} />
        <StatCard label="Percentage" value={formatPercent(result.percentage)} hint={`Pass mark ${result.passingPercentage}%`} />
        <StatCard label="Correct" value={result.correctCount} />
        <StatCard label="Incorrect" value={result.incorrectCount} />
        <StatCard label="Unanswered" value={result.unansweredCount} />
        <StatCard label="Time taken" value={formatDuration(result.timeTakenSeconds)} />
      </div>

      <div className="cf-two-col cf-section">
        <div className="cf-card">
          <h2>Topic-wise performance</h2>
          {result.topicPerformance.map((t) => (
            <div key={t.topic} style={{ marginBottom: 10 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>{t.topic}</span><span className="cf-muted">{t.correct}/{t.total}</span>
              </div>
              <div className="cf-progress"><div style={{ width: `${t.accuracy}%` }} /></div>
            </div>
          ))}
        </div>
        <div className="cf-card">
          <h2>Topics to revise</h2>
          {result.revisionTopics.length === 0
            ? <p className="cf-muted">No weak topics in this attempt. Nice work.</p>
            : <div className="cf-pill-list">{result.revisionTopics.map((t) => <Badge key={t} tone="warning">{t}</Badge>)}</div>}
          <div style={{ display: 'flex', gap: 8, marginTop: 16, flexWrap: 'wrap' }}>
            {result.canRetake && <Link className="cf-btn cf-btn-primary" to={`/tests/${result.testId}`}>Retake test</Link>}
            <Link className="cf-btn cf-btn-outline" to="/dashboard">Back to dashboard</Link>
            <Link className="cf-btn cf-btn-outline" to="/history">View history</Link>
            <Link className="cf-btn cf-btn-outline"
              to={testListHref({ mode: result.mode, careerPathId: result.careerPathId })}>More tests</Link>
          </div>
        </div>
      </div>

      <h2>Question review</h2>
      {result.review.map((item) => (
        <div className="cf-card" key={item.questionId} style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
            <strong>Q{item.seqNo}. {item.text}</strong>
            <Badge tone={item.correct === true ? 'success' : item.correct === false ? 'danger' : 'neutral'}>
              {item.correct === true ? 'Correct' : item.correct === false ? 'Incorrect' : 'Unanswered'}
            </Badge>
          </div>
          <div style={{ marginTop: 10 }}>
            {item.options.map((o) => (
              <div key={o.id} className={`cf-option ${o.correct ? 'correct' : o.selected ? 'wrong' : ''}`}>
                <span>{o.text}{o.selected ? ' (your answer)' : ''}{o.correct ? ' ✓' : ''}</span>
              </div>
            ))}
          </div>
          {item.explanation && <p className="cf-muted"><strong>Explanation:</strong> {item.explanation}</p>}
        </div>
      ))}
    </>
  );
}
