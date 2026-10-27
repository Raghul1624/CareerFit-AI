import { labelize } from './format.js';

/** Pure helpers for the mock-test screens (kept free of React so they are easy to unit test). */

const DIFFICULTY_ORDER = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

export function modeLabel(mode) {
  return mode === 'CAREER' ? 'Career mock test' : 'Skill test';
}

/** "Beginner to Advanced", "Intermediate", or "Not rated" for a list of difficulty enums. */
export function difficultyLabel(levels) {
  const present = DIFFICULTY_ORDER.filter((level) => (levels || []).includes(level));
  if (present.length === 0) return 'Not rated';
  if (present.length === 1) return labelize(present[0]);
  return `${labelize(present[0])} to ${labelize(present[present.length - 1])}`;
}

/** Splits a long list into the first `max` items and the number that were left out. */
export function summarizeList(items, max = 4) {
  const list = items || [];
  return { shown: list.slice(0, max), hidden: Math.max(0, list.length - max) };
}

/** Link to the instructions screen of a test. */
export function testHref(test) {
  return `/tests/${test.id}`;
}

/** Link to the test list with filters kept in the URL, so the selection survives navigation. */
export function testListHref({ mode, careerPathId, skillId } = {}) {
  const params = new URLSearchParams();
  if (mode) params.set('mode', mode);
  if (careerPathId) params.set('careerPathId', String(careerPathId));
  if (skillId) params.set('skillId', String(skillId));
  const query = params.toString();
  return query ? `/tests?${query}` : '/tests';
}

/** Where "Back" should lead from a test: its career path page, its skill's test list, or all tests. */
export function backLink(test) {
  if (!test) return { to: '/tests', label: 'All mock tests' };
  if (test.mode === 'CAREER' && test.careerPathId) {
    return { to: `/career-paths/${test.careerPathId}`, label: `Back to ${test.careerPathName || 'career path'}` };
  }
  if (test.mode === 'SKILL' && test.skillIds && test.skillIds.length === 1) {
    return { to: testListHref({ mode: 'SKILL', skillId: test.skillIds[0] }), label: 'Back to skill tests' };
  }
  return { to: '/tests', label: 'All mock tests' };
}

/** Case-insensitive search across the visible fields of a test. */
export function matchesSearch(test, text) {
  const needle = (text || '').trim().toLowerCase();
  if (!needle) return true;
  const haystack = [test.title, test.description, test.careerPathName, ...(test.skills || []), ...(test.topics || [])]
    .filter(Boolean).join(' ').toLowerCase();
  return needle.split(/\s+/).every((word) => haystack.includes(word));
}

export function answeredCount(questions, answers) {
  return questions.filter((q) => answers[q.id]?.selected?.length > 0).length;
}

export function markedCount(questions, answers) {
  return questions.filter((q) => answers[q.id]?.marked).length;
}

export function progressPercent(done, total) {
  if (!total) return 0;
  return Math.round((done / total) * 100);
}

/** 'marked' wins over 'answered' so a flagged question stays visible in the palette. */
export function paletteState(answer) {
  if (answer?.marked) return 'marked';
  if (answer?.selected?.length > 0) return 'answered';
  return 'unanswered';
}

/** Text shown in the submit confirmation dialog. */
export function submitSummary(questions, answers) {
  const total = questions.length;
  const answered = answeredCount(questions, answers);
  const marked = markedCount(questions, answers);
  return { total, answered, unanswered: total - answered, marked };
}
