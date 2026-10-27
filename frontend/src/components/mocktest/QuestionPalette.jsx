import { paletteState } from '../../utils/mockTests.js';

/** Numbered buttons for jumping between questions; colours show answered and marked-for-review. */
export default function QuestionPalette({ questions, answers, current, onSelect }) {
  return (
    <>
      <div className="cf-palette">
        {questions.map((q, i) => {
          const state = paletteState(answers[q.id]);
          const cls = [state === 'unanswered' ? '' : state, i === current ? 'current' : ''].join(' ').trim();
          return (
            <button key={q.id} type="button" className={cls} onClick={() => onSelect(i)}
              aria-label={`Question ${i + 1}, ${state}`} aria-current={i === current ? 'true' : undefined}>
              {i + 1}
            </button>
          );
        })}
      </div>
      <p className="cf-muted cf-palette-legend">Green: answered &middot; Yellow: marked for review &middot; White: not answered</p>
    </>
  );
}
