import { describe, expect, it } from 'vitest';
import {
  answeredCount, backLink, difficultyLabel, markedCount, matchesSearch, modeLabel, paletteState,
  progressPercent, submitSummary, summarizeList, testHref, testListHref,
} from './mockTests.js';

describe('mock test helpers', () => {
  it('labels the test type', () => {
    expect(modeLabel('CAREER')).toBe('Career mock test');
    expect(modeLabel('SKILL')).toBe('Skill test');
  });

  it('describes the difficulty range in order, whatever order the API sends', () => {
    expect(difficultyLabel(['ADVANCED', 'BEGINNER', 'INTERMEDIATE'])).toBe('Beginner to Advanced');
    expect(difficultyLabel(['INTERMEDIATE'])).toBe('Intermediate');
    expect(difficultyLabel(['ADVANCED', 'INTERMEDIATE'])).toBe('Intermediate to Advanced');
    expect(difficultyLabel([])).toBe('Not rated');
    expect(difficultyLabel(undefined)).toBe('Not rated');
  });

  it('shortens long topic lists and reports how many were hidden', () => {
    expect(summarizeList(['a', 'b', 'c'], 2)).toEqual({ shown: ['a', 'b'], hidden: 1 });
    expect(summarizeList(['a'], 4)).toEqual({ shown: ['a'], hidden: 0 });
    expect(summarizeList(null)).toEqual({ shown: [], hidden: 0 });
  });

  it('builds links that keep the selected test type, career path and skill', () => {
    expect(testHref({ id: 7 })).toBe('/tests/7');
    expect(testListHref()).toBe('/tests');
    expect(testListHref({ mode: 'CAREER', careerPathId: 6 })).toBe('/tests?mode=CAREER&careerPathId=6');
    expect(testListHref({ mode: 'SKILL', skillId: 4 })).toBe('/tests?mode=SKILL&skillId=4');
  });

  it('chooses a sensible back link for each kind of test', () => {
    expect(backLink({ mode: 'CAREER', careerPathId: 1, careerPathName: 'Java Developer' }))
      .toEqual({ to: '/career-paths/1', label: 'Back to Java Developer' });
    expect(backLink({ mode: 'SKILL', skillIds: [4] }).to).toBe('/tests?mode=SKILL&skillId=4');
    expect(backLink(null).to).toBe('/tests');
  });

  it('searches title, career path, skills and topics, matching every word', () => {
    const test = { title: 'Java Developer Mock Test', careerPathName: 'Java Developer', skills: ['Spring Boot'], topics: ['JDBC'] };
    expect(matchesSearch(test, '')).toBe(true);
    expect(matchesSearch(test, 'jdbc')).toBe(true);
    expect(matchesSearch(test, 'spring java')).toBe(true);
    expect(matchesSearch(test, 'python')).toBe(false);
  });

  it('counts answered and marked questions and builds the submit summary', () => {
    const questions = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }];
    const answers = { 1: { selected: [10], marked: false }, 2: { selected: [], marked: true }, 3: { selected: [5], marked: true } };
    expect(answeredCount(questions, answers)).toBe(2);
    expect(markedCount(questions, answers)).toBe(2);
    expect(submitSummary(questions, answers)).toEqual({ total: 4, answered: 2, unanswered: 2, marked: 2 });
  });

  it('gives marked-for-review priority in the palette', () => {
    expect(paletteState({ selected: [1], marked: true })).toBe('marked');
    expect(paletteState({ selected: [1], marked: false })).toBe('answered');
    expect(paletteState({ selected: [], marked: false })).toBe('unanswered');
    expect(paletteState(undefined)).toBe('unanswered');
  });

  it('computes progress without dividing by zero', () => {
    expect(progressPercent(5, 20)).toBe(25);
    expect(progressPercent(0, 0)).toBe(0);
  });
});
