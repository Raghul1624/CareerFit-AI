import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { getCodingStatus, getRole, startInterview } from '../../api/interview.js';
import { Badge, Button, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { findRound, interviewAttemptPath, levelLabel, levelTone } from '../../utils/interview.js';

const INSTRUCTIONS = {
  APTITUDE: [
    'Multiple-choice questions on logical reasoning and quantitative aptitude.',
    'Move between questions freely, mark questions for review and use the palette.',
    'Each question is worth 1 mark and is marked on the server. There is no negative marking.',
    'Correct answers and explanations appear after you submit.',
  ],
  TECHNICAL_MCQ: [
    'Multiple-choice questions on the technical skills of your role.',
    'Some questions need several correct options (they say "select all that apply").',
    'Each question is worth 1 mark and is marked on the server. There is no negative marking.',
    'Correct answers and explanations appear after you submit.',
  ],
  CODING: [
    'Solve programming problems in Java, Python or JavaScript. Programs read standard input and print to standard output.',
    'Run code executes the sample test cases (or your own input). Submit code judges it against the sample AND hidden test cases.',
    'Your score for a problem is the share of test cases your best submission passes. Only submitted code is scored.',
    'Code is executed in an external sandbox, never on the application server.',
  ],
  TECHNICAL_INTERVIEW: [
    'Answer each question in your own words, as you would in a technical interview.',
    'Your answers are scored with a transparent rubric: Correctness, Completeness, Clarity and Technical understanding. No AI is used.',
    'Aim for 60 to 150 words per answer, with a short example.',
    'A model answer and detailed feedback appear after you submit.',
  ],
  HR_INTERVIEW: [
    'Answer each question as you would speak it in an HR interview.',
    'Your answers are scored with a transparent rubric: Relevance, Structure, Specificity and Communication. No AI is used.',
    'Use real examples and the STAR method (Situation, Task, Action, Result), and include numbers where you can.',
    'A sample answer and improvement suggestions appear after you submit.',
  ],
};

export default function InterviewSetup() {
  const { roleCode, round: roundCode } = useParams();
  const navigate = useNavigate();
  const [role, setRole] = useState(null);
  const [level, setLevel] = useState('');
  const [coding, setCoding] = useState(null);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    getRole(roleCode).then(setRole).catch((e) => setError(errorMessage(e)));
    if (roundCode === 'CODING') getCodingStatus().then(setCoding).catch(() => setCoding({ configured: false, message: 'Could not check the code execution service.' }));
  }, [roleCode, roundCode]);

  const round = role ? findRound(role, roundCode) : null;
  useEffect(() => {
    if (round && !level) setLevel((round.levels.find((l) => l.available) || {}).level || '');
  }, [round, level]);

  if (error && !role) return <><ErrorMessage message={error} /><p><Link to="/interview">Back to roles</Link></p></>;
  if (!role) return <LoadingSpinner />;
  if (!round) return <><ErrorMessage message="That round does not exist." /><p><Link to={`/interview/${roleCode}`}>Back to rounds</Link></p></>;

  const chosen = round.levels.find((l) => l.level === level);
  const sandboxBlocked = roundCode === 'CODING' && coding && !coding.configured;

  const start = async () => {
    setStarting(true);
    setError('');
    try {
      const attempt = await startInterview(role.code, roundCode, level);
      navigate(interviewAttemptPath(attempt.id));
    } catch (e) {
      setError(errorMessage(e));
      setStarting(false);
    }
  };

  return (
    <>
      <p><Link to={`/interview/${role.code}`}>&larr; {role.name} rounds</Link></p>
      <PageHeader title={round.label} subtitle={`${role.name} · ${round.description}`} />
      <ErrorMessage message={error} />
      {sandboxBlocked && <div className="cf-alert cf-alert-warn" role="alert"><strong>Code execution is not configured. </strong>{coding.message}</div>}

      <div className="cf-two-col">
        <div className="cf-card">
          <h2>Choose a difficulty</h2>
          <div className="cf-level-list" role="radiogroup" aria-label="Difficulty level">
            {round.levels.map((l) => (
              <label key={l.level} className={`cf-level ${level === l.level ? 'selected' : ''} ${l.available ? '' : 'disabled'}`}>
                <input type="radio" name="level" value={l.level} checked={level === l.level} disabled={!l.available} onChange={() => setLevel(l.level)} />
                <div>
                  <div><Badge tone={levelTone(l.level)}>{levelLabel(l.level)}</Badge></div>
                  {l.available
                    ? <small className="cf-muted">{l.questionCount} {roundCode === 'CODING' ? 'problems' : 'questions'} &middot; {l.durationMinutes} minutes &middot; pass mark {l.passingPercentage}%</small>
                    : <small className="cf-muted">{l.reason}</small>}
                </div>
              </label>
            ))}
          </div>
          <Button onClick={start} loading={starting} disabled={!chosen?.available || sandboxBlocked}>Start assessment</Button>
          <p className="cf-muted" style={{ marginTop: 8 }}>The timer starts when you press Start. An unfinished attempt of the same role, round and level is resumed instead of starting a new one.</p>
        </div>

        <div className="cf-card">
          <h2>Instructions</h2>
          {chosen?.available && (
            <dl className="cf-details">
              <dt>Role</dt><dd>{role.name}</dd>
              <dt>Round</dt><dd>{round.label}</dd>
              <dt>Level</dt><dd>{levelLabel(chosen.level)}</dd>
              <dt>{roundCode === 'CODING' ? 'Problems' : 'Questions'}</dt><dd>{chosen.questionCount}</dd>
              <dt>Duration</dt><dd>{chosen.durationMinutes} minutes</dd>
              <dt>Pass mark</dt><dd>{chosen.passingPercentage}%</dd>
            </dl>
          )}
          <ul className="cf-instructions">{(INSTRUCTIONS[roundCode] || []).map((t) => <li key={t}>{t}</li>)}</ul>
          <ul className="cf-instructions">
            <li>The server keeps the clock. Refreshing the page does not add time, and the attempt is submitted automatically at zero.</li>
            <li>Your answers are saved automatically every few seconds.</li>
          </ul>
        </div>
      </div>
    </>
  );
}
