import { Badge, Button } from '../ui.jsx';
import QuestionPalette from '../mocktest/QuestionPalette.jsx';
import { paletteAnswers, wordCount } from '../../utils/interview.js';

const MAX_CHARS = 5000;

/** Technical Interview and HR Interview: a text-answer editor per question, with guidance and a live word count. */
export default function TextAssessment({ attempt, answers, update, index, setIndex, onSubmitClick, submitting }) {
  const q = attempt.questions[index];
  const a = answers[q.id] || { text: '', marked: false };
  const words = wordCount(a.text);
  const isHr = attempt.round === 'HR_INTERVIEW';
  const hint = words === 0 ? 'Write your answer in your own words.' : words < 40 ? 'Add more detail: a fuller answer scores better.' : words > 300 ? 'Consider shortening: keep the key points clear.' : 'Good length.';

  return (
    <div className="cf-test-layout">
      <div className="cf-card">
        <p className="cf-muted">
          Question {index + 1} of {attempt.totalQuestions}
          {q.topic && <> &middot; <Badge>{q.topic}</Badge></>}
        </p>
        <h2 style={{ whiteSpace: 'pre-wrap' }}>{q.text}</h2>
        {q.guidance && <div className="cf-alert cf-alert-info" role="note"><strong>Tip: </strong>{q.guidance}</div>}
        {!q.guidance && !isHr && <div className="cf-alert cf-alert-info" role="note"><strong>Tip: </strong>Explain the idea, give a short example and say why it matters. Aim for 60 to 150 words.</div>}
        <label htmlFor={`answer-${q.id}`} className="cf-muted">Your answer</label>
        <textarea id={`answer-${q.id}`} className="cf-input cf-answer-box" rows={11} maxLength={MAX_CHARS} value={a.text}
          placeholder={isHr ? 'Type your answer as you would say it in an interview...' : 'Type your explanation here...'}
          onChange={(e) => update(q.id, { text: e.target.value })} />
        <div className="cf-muted cf-answer-meta">
          <span>{words} words &middot; {a.text.length}/{MAX_CHARS} characters</span>
          <span>{hint}</span>
        </div>
        <div className="cf-actions-row">
          <Button variant="outline" onClick={() => setIndex(index - 1)} disabled={index === 0}>Previous</Button>
          <Button variant="outline" onClick={() => setIndex(index + 1)} disabled={index === attempt.questions.length - 1}>Next</Button>
          <Button variant="outline" onClick={() => update(q.id, { marked: !a.marked })}>{a.marked ? 'Unmark review' : 'Mark for review'}</Button>
        </div>
      </div>

      <div className="cf-card">
        <h3>Questions</h3>
        <QuestionPalette questions={attempt.questions} answers={paletteAnswers(attempt.questions, answers)} current={index} onSelect={setIndex} />
        <Button onClick={onSubmitClick} loading={submitting}>Submit attempt</Button>
      </div>
    </div>
  );
}
