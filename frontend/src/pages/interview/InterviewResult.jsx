import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { getResult } from '../../api/interview.js';
import { Badge, ErrorMessage, LoadingSpinner, PageHeader, StatCard } from '../../components/ui.jsx';
import ResultItemCard from '../../components/interview/ResultItemCard.jsx';
import { formatDuration, formatPercent } from '../../utils/format.js';
import { interviewAttemptPath, levelLabel, levelTone } from '../../utils/interview.js';

export default function InterviewResult() {
  const { id } = useParams();
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [attemptOpen, setAttemptOpen] = useState(false);

  useEffect(() => {
    getResult(id).then(setResult).catch((e) => {
      if (e.response?.status === 409) setAttemptOpen(true);
      else setError(errorMessage(e));
    });
  }, [id]);

  if (attemptOpen) {
    return (
      <>
        <div className="cf-alert cf-alert-warn">This attempt is still in progress. Results are available after you submit.</div>
        <Link className="cf-btn cf-btn-primary" to={interviewAttemptPath(id)}>Resume attempt</Link>
      </>
    );
  }
  if (error) return <><ErrorMessage message={error} /><p><Link to="/interview">Back to Interview Preparation</Link></p></>;
  if (!result) return <LoadingSpinner label="Loading your results..." />;

  const isMcq = result.round === 'APTITUDE' || result.round === 'TECHNICAL_MCQ';
  const unit = result.round === 'CODING' ? 'problems' : 'questions';
  return (
    <>
      <PageHeader title={`${result.roundLabel} results`} subtitle={`${result.roleName} · ${levelLabel(result.level)}`}>
        <Badge tone={result.passed ? 'success' : 'danger'}>{result.passed ? 'Passed' : 'Not passed yet'}</Badge>
      </PageHeader>
      <div className="cf-test-card-badges" style={{ marginBottom: 12 }}>
        <Badge tone="success">{result.roleName}</Badge>
        <Badge tone={levelTone(result.level)}>{levelLabel(result.level)}</Badge>
        {result.status === 'AUTO_SUBMITTED' && <Badge tone="warning">Auto-submitted when time ran out</Badge>}
      </div>

      <div className="cf-grid-stats">
        <StatCard label="Score" value={isMcq ? `${result.score} / ${result.maxScore}` : `${Math.round(result.percentage)}%`} hint={`Pass mark ${result.passingPercentage}%`} />
        <StatCard label="Percentage" value={formatPercent(result.percentage)} />
        <StatCard label={isMcq ? 'Correct answers' : `Good ${unit}`} value={`${result.correctCount} / ${result.totalQuestions}`} />
        <StatCard label="Time taken" value={result.timeTakenSeconds == null ? '-' : formatDuration(result.timeTakenSeconds)} />
      </div>

      <div className="cf-card" style={{ marginBottom: 16 }}>
        <h2>Feedback</h2>
        <p>{result.summaryFeedback}</p>
        <details><summary>How this was scored</summary><p className="cf-muted">{result.scoringMethod}</p></details>
      </div>

      {result.topicPerformance.length > 1 && (
        <div className="cf-card" style={{ marginBottom: 16 }}>
          <h2>Performance by topic</h2>
          {result.topicPerformance.map((t) => {
            const pct = t.total === 0 ? 0 : Math.round((t.correct / t.total) * 100);
            return (
              <div key={t.topic} className="cf-scorebar">
                <div className="cf-scorebar-head"><span>{t.topic}</span><strong>{t.correct}/{t.total}</strong></div>
                <div className="cf-progress"><div className={`cf-bar-${pct >= 70 ? 'success' : pct >= 40 ? 'warning' : 'danger'}`} style={{ width: `${pct}%` }} /></div>
              </div>
            );
          })}
        </div>
      )}

      <div className="cf-actions-row" style={{ marginBottom: 16 }}>
        <Link className="cf-btn cf-btn-primary" to={`/interview/${result.roleCode}/${result.round}`}>Retake this round</Link>
        <Link className="cf-btn cf-btn-outline" to={`/interview/${result.roleCode}`}>Try another round</Link>
        <Link className="cf-btn cf-btn-outline" to="/interview/history">Attempt history</Link>
        <Link className="cf-btn cf-btn-outline" to="/dashboard">Back to dashboard</Link>
      </div>

      <h2>Review</h2>
      {result.items.map((item) => <ResultItemCard key={item.questionId} item={item} round={result.round} />)}
    </>
  );
}
