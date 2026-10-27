import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, Button, ErrorMessage, LoadingSpinner } from '../../components/ui.jsx';
import ConfirmDialog from '../../components/mocktest/ConfirmDialog.jsx';
import QuestionPalette from '../../components/mocktest/QuestionPalette.jsx';
import { formatDuration } from '../../utils/format.js';
import { answeredCount, modeLabel, progressPercent, submitSummary } from '../../utils/mockTests.js';

export default function ActiveTest() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({});
  const [index, setIndex] = useState(0);
  const [remaining, setRemaining] = useState(null);
  const [saveState, setSaveState] = useState('saved');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [switches, setSwitches] = useState(0);
  const [confirming, setConfirming] = useState(false);

  const answersRef = useRef({});
  const dirty = useRef(false);
  const offset = useRef(0);          // serverTime - clientTime, so the countdown follows the server clock
  const submitted = useRef(false);

  // Load the attempt (the server returns the persisted question order, never the answer key).
  useEffect(() => {
    api.get(`/attempts/${id}`)
      .then(({ data }) => {
        offset.current = new Date(data.serverTime).getTime() - Date.now();
        const initial = {};
        data.questions.forEach((q) => {
          initial[q.id] = { selected: q.selectedOptionIds || [], marked: q.markedForReview };
        });
        answersRef.current = initial;
        setAnswers(initial);
        setAttempt(data);
      })
      .catch((e) => {
        if (e.response?.status === 409) navigate(`/results/${id}`, { replace: true });
        else setError(errorMessage(e));
      });
  }, [id, navigate]);

  const flush = useCallback(async () => {
    if (!dirty.current) return true;
    dirty.current = false;
    setSaveState('saving');
    try {
      await api.put(`/attempts/${id}/answers`, {
        answers: Object.entries(answersRef.current).map(([qid, a]) => ({
          questionId: Number(qid), selectedOptionIds: a.selected, markedForReview: a.marked,
        })),
      });
      setSaveState('saved');
      return true;
    } catch (e) {
      dirty.current = true;
      setSaveState('error');
      if (e.response?.status === 409) navigate(`/results/${id}`, { replace: true });
      return false;
    }
  }, [id, navigate]);

  const submit = useCallback(async () => {
    if (submitted.current) return;
    submitted.current = true;
    setSubmitting(true);
    await flush();
    try {
      await api.post(`/attempts/${id}/submit`);
    } catch (e) {
      if (e.response?.status !== 409) {
        submitted.current = false;
        setSubmitting(false);
        setError(errorMessage(e));
        return;
      }
    }
    navigate(`/results/${id}`, { replace: true });
  }, [flush, id, navigate]);

  // Countdown from the server-provided expiry time.
  useEffect(() => {
    if (!attempt) return undefined;
    const expires = new Date(attempt.expiresAt).getTime();
    const tick = () => {
      const left = Math.round((expires - (Date.now() + offset.current)) / 1000);
      setRemaining(Math.max(0, left));
      if (left <= 0) submit();
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [attempt, submit]);

  // Autosave every 5 seconds when something changed.
  useEffect(() => {
    const timer = setInterval(() => { flush(); }, 5000);
    return () => clearInterval(timer);
  }, [flush]);

  // Best-effort tab-switch notice. Browsers cannot reliably prevent or detect all cheating.
  useEffect(() => {
    const onVisibility = () => { if (document.hidden) setSwitches((s) => s + 1); };
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, []);

  const update = (qid, patch) => {
    const next = { ...answersRef.current, [qid]: { ...answersRef.current[qid], ...patch } };
    answersRef.current = next;
    dirty.current = true;
    setSaveState('unsaved');
    setAnswers(next);
  };

  const choose = (question, optionId) => {
    const current = answersRef.current[question.id]?.selected || [];
    if (question.type === 'MULTIPLE_CHOICE') {
      const selected = current.includes(optionId) ? current.filter((x) => x !== optionId) : [...current, optionId];
      update(question.id, { selected });
    } else {
      update(question.id, { selected: [optionId] });
    }
  };

  if (error && !attempt) return <div className="cf-content"><ErrorMessage message={error} /></div>;
  if (!attempt) return <LoadingSpinner label="Loading your test..." />;

  const q = attempt.questions[index];
  const a = answers[q.id] || { selected: [], marked: false };
  const answered = answeredCount(attempt.questions, answers);
  const summary = submitSummary(attempt.questions, answers);
  const saveText = saveState === 'saved' ? 'All changes saved' : saveState === 'saving' ? 'Saving...'
    : saveState === 'error' ? 'Could not save, will retry' : 'Unsaved changes';

  return (
    <div className="cf-content">
      <div className="cf-card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <div>
            <strong>{attempt.title}</strong>
            <div className="cf-test-card-badges" style={{ marginTop: 4 }}>
              <Badge tone={attempt.mode === 'CAREER' ? 'success' : 'neutral'}>{modeLabel(attempt.mode)}</Badge>
              {attempt.careerPathName && <Badge>{attempt.careerPathName}</Badge>}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className={`cf-timer ${remaining !== null && remaining < 60 ? 'low' : ''}`} aria-label="Time remaining">
              {remaining === null ? '--:--' : formatDuration(remaining)}
            </div>
            <small className="cf-muted">Time remaining</small>
          </div>
        </div>
        <div className="cf-progress" style={{ marginTop: 12 }} role="progressbar" aria-label="Questions answered"
          aria-valuemin={0} aria-valuemax={attempt.totalQuestions} aria-valuenow={answered}>
          <div style={{ width: `${progressPercent(answered, attempt.totalQuestions)}%` }} />
        </div>
        <div className="cf-muted" style={{ marginTop: 6 }}>
          Answered {answered} of {attempt.totalQuestions} &middot; {saveText}
        </div>
      </div>

      {switches > 0 && (
        <div className="cf-alert cf-alert-warn">You left this tab {switches} time(s). Please stay on the test page.</div>
      )}
      <ErrorMessage message={error} />

      <div className="cf-test-layout">
        <div className="cf-card">
          <p className="cf-muted">
            Question {index + 1} of {attempt.totalQuestions}
            {q.type === 'MULTIPLE_CHOICE' ? ' \u00b7 select all that apply' : ''}
          </p>
          <h2 style={{ whiteSpace: 'pre-wrap' }}>{q.text}</h2>
          {q.options.map((o) => {
            const selected = a.selected.includes(o.id);
            return (
              <label key={o.id} className={`cf-option ${selected ? 'selected' : ''}`}>
                <input type={q.type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'} name={`q-${q.id}`}
                  checked={selected} onChange={() => choose(q, o.id)} />
                <span>{o.text}</span>
              </label>
            );
          })}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 16 }}>
            <Button variant="outline" onClick={() => setIndex(index - 1)} disabled={index === 0}>Previous</Button>
            <Button variant="outline" onClick={() => setIndex(index + 1)} disabled={index === attempt.questions.length - 1}>Next</Button>
            <Button variant="outline" onClick={() => update(q.id, { marked: !a.marked })}>{a.marked ? 'Unmark review' : 'Mark for review'}</Button>
            <Button variant="outline" onClick={() => update(q.id, { selected: [] })} disabled={a.selected.length === 0}>Clear answer</Button>
          </div>
        </div>

        <div className="cf-card">
          <h3>Question palette</h3>
          <QuestionPalette questions={attempt.questions} answers={answers} current={index} onSelect={setIndex} />
          <Button onClick={() => setConfirming(true)} loading={submitting}>Submit test</Button>
        </div>
      </div>

      <ConfirmDialog open={confirming} title="Submit your test?" confirmLabel="Submit test" cancelLabel="Keep working"
        busy={submitting} onCancel={() => setConfirming(false)} onConfirm={() => { setConfirming(false); submit(); }}>
        <p>You have answered <strong>{summary.answered}</strong> of <strong>{summary.total}</strong> questions.</p>
        {summary.unanswered > 0 && <p className="cf-alert cf-alert-warn">{summary.unanswered} question(s) are unanswered and will score 0.</p>}
        {summary.marked > 0 && <p>{summary.marked} question(s) are still marked for review.</p>}
        <p className="cf-muted">You cannot change your answers after submitting.</p>
      </ConfirmDialog>
    </div>
  );
}
