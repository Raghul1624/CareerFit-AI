import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client.js';
import { getAttempt, saveAnswers, submitAttempt } from '../api/interview.js';
import { buildSaveBody, initialAnswers, interviewResultPath } from '../utils/interview.js';

/**
 * State and behaviour shared by every interview assessment page: loading the attempt, the answers, autosave every
 * 5 seconds, the countdown that follows the SERVER clock, and submitting.
 * A finished attempt (submitted, or auto-submitted when time ran out) redirects to its result page.
 */
export default function useInterviewSession(attemptId) {
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({});
  const [remaining, setRemaining] = useState(null);
  const [saveState, setSaveState] = useState('saved');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [tabSwitches, setTabSwitches] = useState(0);

  const answersRef = useRef({});
  const attemptRef = useRef(null);
  const dirty = useRef(false);
  const offset = useRef(0);          // serverTime - clientTime, so the countdown follows the server clock
  const submitted = useRef(false);

  const goToResult = useCallback(() => navigate(interviewResultPath(attemptId), { replace: true }), [navigate, attemptId]);

  useEffect(() => {
    getAttempt(attemptId)
      .then((data) => {
        if (data.status !== 'IN_PROGRESS') { goToResult(); return; }
        offset.current = new Date(data.serverTime).getTime() - Date.now();
        const initial = initialAnswers(data.questions);
        answersRef.current = initial;
        attemptRef.current = data;
        setAnswers(initial);
        setAttempt(data);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [attemptId, goToResult]);

  const flush = useCallback(async () => {
    if (!dirty.current || !attemptRef.current) return true;
    dirty.current = false;
    setSaveState('saving');
    try {
      await saveAnswers(attemptId, buildSaveBody(attemptRef.current.questions, answersRef.current));
      setSaveState('saved');
      return true;
    } catch (e) {
      dirty.current = true;
      if (e.response?.status === 409) { goToResult(); return false; }
      setSaveState('error');
      return false;
    }
  }, [attemptId, goToResult]);

  const submit = useCallback(async () => {
    if (submitted.current) return;
    submitted.current = true;
    setSubmitting(true);
    await flush();
    try {
      await submitAttempt(attemptId);
    } catch (e) {
      if (e.response?.status !== 409) {
        submitted.current = false;
        setSubmitting(false);
        setError(errorMessage(e));
        return;
      }
    }
    goToResult();
  }, [flush, attemptId, goToResult]);

  // Countdown from the server-provided expiry; submits automatically at zero.
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

  useEffect(() => {
    const timer = setInterval(() => { flush(); }, 5000);
    return () => clearInterval(timer);
  }, [flush]);

  // Best-effort notice only: browsers cannot reliably prevent or detect cheating.
  useEffect(() => {
    const onVisibility = () => { if (document.hidden) setTabSwitches((n) => n + 1); };
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, []);

  /** Merge a change into one question's answer and mark the session as unsaved. */
  const update = useCallback((questionId, patch) => {
    const next = { ...answersRef.current, [questionId]: { ...answersRef.current[questionId], ...patch } };
    answersRef.current = next;
    dirty.current = true;
    setSaveState('unsaved');
    setAnswers(next);
  }, []);

  return { attempt, answers, update, remaining, saveState, error, setError, submitting, submit, flush, tabSwitches };
}
