import { Badge } from '../ui.jsx';
import { scoreTone, verdictLabel, verdictTone } from '../../utils/interview.js';

function ScoreBar({ label, score, weight, comment }) {
  return (
    <div className="cf-scorebar">
      <div className="cf-scorebar-head">
        <span>{label} <small className="cf-muted">({weight}% of the score)</small></span>
        <strong>{score}/100</strong>
      </div>
      <div className="cf-progress"><div className={`cf-bar-${scoreTone(score)}`} style={{ width: `${score}%` }} /></div>
      <small className="cf-muted">{comment}</small>
    </div>
  );
}

function McqReview({ item }) {
  return (
    <>
      {item.options.map((o) => {
        const cls = o.correct ? 'correct' : o.selected ? 'wrong' : '';
        return (
          <div key={o.id} className={`cf-option ${cls}`}>
            <span>{o.text}{o.selected && <small className="cf-muted"> (your answer)</small>}{o.correct && <small className="cf-muted"> (correct)</small>}</span>
          </div>
        );
      })}
      {item.options.every((o) => !o.selected) && <p className="cf-muted">You did not answer this question.</p>}
      <div className="cf-explanation"><strong>Explanation: </strong>{item.explanation}</div>
    </>
  );
}

function TextReview({ item, hr }) {
  const ev = item.evaluation;
  return (
    <>
      <h4>Your answer</h4>
      <div className="cf-answer-review">{item.yourAnswer ? item.yourAnswer : <em className="cf-muted">No answer was written.</em>}</div>
      {ev && (
        <>
          <h4>Rubric score: {ev.overall}/100 <small className="cf-muted">({ev.wordCount} words)</small></h4>
          {ev.dimensions.map((d) => <ScoreBar key={d.name} label={d.name} score={d.score} weight={d.weightPercent} comment={d.comment} />)}
          <div className="cf-two-col" style={{ marginTop: 12 }}>
            <div>
              <h4>What went well</h4>
              <ul>{ev.strengths.map((s) => <li key={s}>{s}</li>)}</ul>
            </div>
            <div>
              <h4>How to improve</h4>
              {ev.improvements.length === 0 ? <p className="cf-muted">Nothing major to add.</p> : <ul>{ev.improvements.map((s) => <li key={s}>{s}</li>)}</ul>}
            </div>
          </div>
        </>
      )}
      <div className="cf-explanation"><strong>{hr ? 'Advice and sample answer' : 'Model answer'}: </strong>{item.explanation}</div>
    </>
  );
}

function CodingReview({ item }) {
  const c = item.coding;
  return (
    <>
      <p style={{ whiteSpace: 'pre-wrap' }}>{item.text}</p>
      {c && c.submissionCount > 0 ? (
        <>
          <p>
            Best score <strong>{Math.round(c.bestScorePercent ?? 0)}%</strong> &middot; {c.submissionCount} submission{c.submissionCount === 1 ? '' : 's'}
            {c.lastVerdict && <> &middot; latest: <Badge tone={verdictTone(c.lastVerdict)}>{verdictLabel(c.lastVerdict)}</Badge> ({c.lastPassed}/{c.lastTotal} tests, {c.lastLanguage})</>}
          </p>
          {c.lastSourceCode && <details><summary>Show your latest submitted code</summary><pre className="cf-code-view">{c.lastSourceCode}</pre></details>}
        </>
      ) : <p className="cf-muted">No code was submitted for this problem, so it scored 0. Only submitted code is scored.</p>}
    </>
  );
}

/** One reviewed question: correct answer and explanation, rubric breakdown, or coding outcome depending on the type. */
export default function ResultItemCard({ item, round }) {
  const isText = item.type === 'TEXT';
  const isCoding = item.type === 'CODING';
  const tone = isText ? scoreTone(item.score) : item.correct ? 'success' : isCoding && item.score > 0 ? 'warning' : 'danger';
  const badge = isText ? `${Math.round(item.score)}/100` : isCoding ? `${Math.round(item.score)}%` : item.correct ? 'Correct' : item.options.some((o) => o.selected) ? 'Incorrect' : 'Unanswered';
  return (
    <div className="cf-card cf-result-item">
      <div className="cf-result-head">
        <strong>{item.seqNo}. {isCoding ? item.title : item.topic || 'Question'}</strong>
        <Badge tone={tone}>{badge}</Badge>
      </div>
      {!isCoding && <p className="cf-question-text" style={{ whiteSpace: 'pre-wrap' }}>{item.text}</p>}
      {item.type === 'TEXT' ? <TextReview item={item} hr={round === 'HR_INTERVIEW'} /> : isCoding ? <CodingReview item={item} /> : <McqReview item={item} />}
    </div>
  );
}
