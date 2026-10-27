import { Badge, Button } from '../ui.jsx';
import QuestionPalette from '../mocktest/QuestionPalette.jsx';
import { paletteAnswers } from '../../utils/interview.js';

/** Aptitude and Technical MCQ: one question at a time with a navigation palette. */
export default function McqAssessment({ attempt, answers, update, index, setIndex, onSubmitClick, submitting }) {
  const q = attempt.questions[index];
  const a = answers[q.id] || { selected: [], marked: false };
  const multiple = q.type === 'MULTIPLE_CHOICE';

  const choose = (optionId) => {
    if (multiple) {
      const selected = a.selected.includes(optionId) ? a.selected.filter((x) => x !== optionId) : [...a.selected, optionId];
      update(q.id, { selected });
    } else {
      update(q.id, { selected: [optionId] });
    }
  };

  return (
    <div className="cf-test-layout">
      <div className="cf-card">
        <p className="cf-muted">
          Question {index + 1} of {attempt.totalQuestions}
          {q.topic && <> &middot; <Badge>{q.topic}</Badge></>}
          {multiple && ' · select all that apply'}
        </p>
        <h2 style={{ whiteSpace: 'pre-wrap' }}>{q.text}</h2>
        <div role={multiple ? 'group' : 'radiogroup'} aria-label="Answer options">
          {q.options.map((o) => {
            const selected = a.selected.includes(o.id);
            return (
              <label key={o.id} className={`cf-option ${selected ? 'selected' : ''}`}>
                <input type={multiple ? 'checkbox' : 'radio'} name={`q-${q.id}`} checked={selected} onChange={() => choose(o.id)} />
                <span>{o.text}</span>
              </label>
            );
          })}
        </div>
        <div className="cf-actions-row">
          <Button variant="outline" onClick={() => setIndex(index - 1)} disabled={index === 0}>Previous</Button>
          <Button variant="outline" onClick={() => setIndex(index + 1)} disabled={index === attempt.questions.length - 1}>Next</Button>
          <Button variant="outline" onClick={() => update(q.id, { marked: !a.marked })}>{a.marked ? 'Unmark review' : 'Mark for review'}</Button>
          <Button variant="outline" onClick={() => update(q.id, { selected: [] })} disabled={a.selected.length === 0}>Clear answer</Button>
        </div>
      </div>

      <div className="cf-card">
        <h3>Question palette</h3>
        <QuestionPalette questions={attempt.questions} answers={paletteAnswers(attempt.questions, answers)} current={index} onSelect={setIndex} />
        <Button onClick={onSubmitClick} loading={submitting}>Submit attempt</Button>
      </div>
    </div>
  );
}
