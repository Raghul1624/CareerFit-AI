import { labelize } from './format.js';

/** Pure helpers for the Interview Preparation screens. */

export const ROUND_ORDER = ['APTITUDE', 'TECHNICAL_MCQ', 'CODING', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW'];
export const LEVEL_ORDER = ['EASY', 'MEDIUM', 'HARD'];

export function levelLabel(level) {
  return labelize(level || '');
}

export function levelTone(level) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[level] || 'neutral';
}

/** Short names for the five rounds, used in the history table and chips. */
export function roundShortLabel(round) {
  return {
    APTITUDE: 'Aptitude', TECHNICAL_MCQ: 'Technical MCQ', CODING: 'Coding',
    TECHNICAL_INTERVIEW: 'Technical Interview', HR_INTERVIEW: 'HR Interview',
  }[round] || labelize(round || '');
}

/** Whether the candidate has done something for a question (drives the palette and the submit summary). */
export function isAnswered(question, answer) {
  if (!answer) return false;
  if (question.type === 'CODING') return Boolean(answer.submitted) || (answer.text || '').trim().length > 0 && answer.text !== answer.starter;
  if (question.type === 'TEXT') return (answer.text || '').trim().length > 0;
  return (answer.selected || []).length > 0;
}

export function answeredTotal(questions, answers) {
  return questions.filter((q) => isAnswered(q, answers[q.id])).length;
}

export function markedTotal(questions, answers) {
  return questions.filter((q) => answers[q.id]?.marked).length;
}

/** The shape the QuestionPalette component expects ({selected, marked}), derived from any kind of answer. */
export function paletteAnswers(questions, answers) {
  const out = {};
  questions.forEach((q) => {
    out[q.id] = { selected: isAnswered(q, answers[q.id]) ? [1] : [], marked: Boolean(answers[q.id]?.marked) };
  });
  return out;
}

/** Initial answer state from what the server saved (resume after reload). */
export function initialAnswers(questions) {
  const out = {};
  questions.forEach((q) => {
    const s = q.saved || {};
    out[q.id] = {
      selected: s.selectedOptionIds || [],
      text: s.answerText || '',
      language: s.language || null,
      marked: Boolean(s.markedForReview),
    };
  });
  return out;
}

/** Request body for PUT /answers: only the fields that make sense for each question type. */
export function buildSaveBody(questions, answers) {
  return questions.map((q) => {
    const a = answers[q.id] || {};
    if (q.type === 'TEXT') return { questionId: q.id, answerText: a.text || '', markedForReview: Boolean(a.marked) };
    if (q.type === 'CODING') return { questionId: q.id, answerText: a.text || '', language: a.language || undefined, markedForReview: Boolean(a.marked) };
    return { questionId: q.id, selectedOptionIds: a.selected || [], markedForReview: Boolean(a.marked) };
  });
}

/** Progress sentence under the progress bar: coding problems count as 'worked on', everything else as 'answered'. */
export function progressLabel(round, done, total) {
  return round === 'CODING' ? `Worked on ${done} of ${total} problems` : `Answered ${done} of ${total}`;
}

export function wordCount(text) {
  const t = (text || '').trim();
  return t ? t.split(/\s+/).length : 0;
}

export function verdictLabel(verdict) {
  return labelize(verdict || '');
}

export function verdictTone(verdict) {
  if (verdict === 'PASSED' || verdict === 'ACCEPTED') return 'success';
  if (verdict === 'PARTIAL') return 'warning';
  return 'danger';
}

/** Colour band of a 0-100 score, shared by the rubric bars. */
export function scoreTone(score) {
  if (score >= 70) return 'success';
  if (score >= 40) return 'warning';
  return 'danger';
}

/** Find one round of a role payload. */
export function findRound(role, round) {
  return (role?.rounds || []).find((r) => r.round === round);
}

/** A round is startable when at least one level is available. */
export function roundAvailable(round) {
  return (round?.levels || []).some((l) => l.available);
}

export function interviewResultPath(id) {
  return `/interview/attempts/${id}/result`;
}

export function interviewAttemptPath(id) {
  return `/interview/attempts/${id}`;
}
