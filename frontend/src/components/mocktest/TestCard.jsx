import { Link } from 'react-router-dom';
import { Badge } from '../ui.jsx';
import { difficultyLabel, modeLabel, summarizeList, testHref } from '../../utils/mockTests.js';

/** One test in the list: title, career or skill, topics, difficulty, size, duration and a start button. */
export default function TestCard({ test }) {
  const { shown, hidden } = summarizeList(test.topics, 5);
  const isCareer = test.mode === 'CAREER';
  const scope = isCareer ? test.careerPathName : (test.skills && test.skills[0]);

  return (
    <div className="cf-card cf-test-card">
      <div className="cf-test-card-badges">
        <Badge tone={isCareer ? 'success' : 'neutral'}>{modeLabel(test.mode)}</Badge>
        <Badge tone="warning">{difficultyLabel(test.difficulties)}</Badge>
      </div>
      <h3>{test.title}</h3>
      {scope && <p className="cf-test-scope">{isCareer ? 'Career path' : 'Skill'}: <strong>{scope}</strong></p>}
      <p className="cf-muted">{test.description}</p>

      {shown.length > 0 && (
        <div>
          <small className="cf-muted">Topics covered</small>
          <div className="cf-pill-list" style={{ marginTop: 4 }}>
            {shown.map((t) => <Badge key={t}>{t}</Badge>)}
            {hidden > 0 && <Badge>+{hidden} more</Badge>}
          </div>
        </div>
      )}

      <p className="cf-muted cf-test-meta">
        {test.questionCount} questions &middot; {test.durationMinutes} min &middot; {test.availableQuestions} in the question bank
      </p>

      {test.startable
        ? <Link className="cf-btn cf-btn-primary cf-btn-sm" to={testHref(test)}>Start test</Link>
        : <span className="cf-muted" role="note">{test.unavailableReason}</span>}
    </div>
  );
}
