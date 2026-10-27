import { describe, expect, it } from 'vitest';
import {
  answeredTotal, buildSaveBody, findRound, initialAnswers, isAnswered, levelLabel, markedTotal, paletteAnswers,
  roundAvailable, roundShortLabel, scoreTone, verdictTone, wordCount,
} from './interview.js';
import { escapeHtml, highlightCode, indentForNewLine } from './codeHighlight.js';

const mcq = { id: 1, type: 'SINGLE_CHOICE' };
const text = { id: 2, type: 'TEXT' };
const code = { id: 3, type: 'CODING' };

describe('interview helpers', () => {
  it('labels rounds and levels', () => {
    expect(levelLabel('MEDIUM')).toBe('Medium');
    expect(roundShortLabel('TECHNICAL_MCQ')).toBe('Technical MCQ');
    expect(roundShortLabel('HR_INTERVIEW')).toBe('HR Interview');
  });

  it('decides when a question counts as answered, per type', () => {
    expect(isAnswered(mcq, { selected: [5] })).toBe(true);
    expect(isAnswered(mcq, { selected: [] })).toBe(false);
    expect(isAnswered(text, { text: '  hello ' })).toBe(true);
    expect(isAnswered(text, { text: '   ' })).toBe(false);
    expect(isAnswered(code, { text: 'print(1)', starter: 'import sys' })).toBe(true);
    expect(isAnswered(code, { text: 'import sys', starter: 'import sys' })).toBe(false);
    expect(isAnswered(code, { text: 'import sys', starter: 'import sys', submitted: true })).toBe(true);
    expect(isAnswered(mcq, undefined)).toBe(false);
  });

  it('counts answered and marked questions and derives palette answers', () => {
    const qs = [mcq, text];
    const answers = { 1: { selected: [2], marked: true }, 2: { text: '', marked: false } };
    expect(answeredTotal(qs, answers)).toBe(1);
    expect(markedTotal(qs, answers)).toBe(1);
    expect(paletteAnswers(qs, answers)).toEqual({ 1: { selected: [1], marked: true }, 2: { selected: [], marked: false } });
  });

  it('restores saved answers and builds the save request per question type', () => {
    const qs = [{ id: 1, type: 'MULTIPLE_CHOICE', saved: { selectedOptionIds: [3, 4], markedForReview: true } },
      { id: 2, type: 'TEXT', saved: { answerText: 'my answer' } }, { id: 3, type: 'CODING', saved: { answerText: 'code', language: 'PYTHON' } }];
    const answers = initialAnswers(qs);
    expect(answers[1]).toEqual({ selected: [3, 4], text: '', language: null, marked: true });
    expect(buildSaveBody(qs, answers)).toEqual([
      { questionId: 1, selectedOptionIds: [3, 4], markedForReview: true },
      { questionId: 2, answerText: 'my answer', markedForReview: false },
      { questionId: 3, answerText: 'code', language: 'PYTHON', markedForReview: false },
    ]);
  });

  it('counts words', () => {
    expect(wordCount('  one two   three ')).toBe(3);
    expect(wordCount('')).toBe(0);
    expect(wordCount(null)).toBe(0);
  });

  it('maps verdicts and scores to colour tones', () => {
    expect(verdictTone('PASSED')).toBe('success');
    expect(verdictTone('ACCEPTED')).toBe('success');
    expect(verdictTone('PARTIAL')).toBe('warning');
    expect(verdictTone('WRONG_ANSWER')).toBe('danger');
    expect(scoreTone(85)).toBe('success');
    expect(scoreTone(50)).toBe('warning');
    expect(scoreTone(10)).toBe('danger');
  });

  it('finds a round and tells whether any level is available', () => {
    const role = { rounds: [{ round: 'CODING', levels: [{ available: false }, { available: true }] }, { round: 'HR_INTERVIEW', levels: [{ available: false }] }] };
    expect(roundAvailable(findRound(role, 'CODING'))).toBe(true);
    expect(roundAvailable(findRound(role, 'HR_INTERVIEW'))).toBe(false);
    expect(findRound(role, 'APTITUDE')).toBeUndefined();
  });
});

describe('code highlighting', () => {
  it('escapes HTML so user code can never inject markup', () => {
    expect(escapeHtml('<img src=x onerror=alert(1)> & "q"')).toBe('&lt;img src=x onerror=alert(1)&gt; &amp; "q"');
    const html = highlightCode('print("<script>alert(1)</script>")', 'PYTHON');
    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
  });

  it('marks keywords, strings, numbers and comments for each language', () => {
    const java = highlightCode('int x = 42; // note\nString s = "hi";', 'JAVA');
    expect(java).toContain('<span class="cm-keyword">int</span>');
    expect(java).toContain('<span class="cm-number">42</span>');
    expect(java).toContain('<span class="cm-comment">// note</span>');
    expect(java).toContain('<span class="cm-string">"hi"</span>');
    const py = highlightCode('def f(): # c\n    return None', 'PYTHON');
    expect(py).toContain('<span class="cm-keyword">def</span>');
    expect(py).toContain('<span class="cm-comment"># c</span>');
    expect(highlightCode('const a = `t`;', 'JAVASCRIPT')).toContain('<span class="cm-string">`t`</span>');
  });

  it('does not highlight keywords inside longer words', () => {
    expect(highlightCode('printer = integer', 'PYTHON')).not.toContain('cm-keyword');
  });

  it('keeps the text intact (removing the markup gives back the escaped source)', () => {
    const src = 'for (int i = 0; i < n; i++) { sum += i; } // a < b && c';
    const stripped = highlightCode(src, 'JAVA').replace(/<[^>]+>/g, '');
    expect(stripped).toBe(escapeHtml(src));
  });

  it('continues the indentation on Enter and adds a level after an opening brace or a colon', () => {
    expect(indentForNewLine('    int x = 1;', 'JAVA')).toBe('    ');
    expect(indentForNewLine('public static void main() {', 'JAVA')).toBe('    ');
    expect(indentForNewLine('def f():', 'PYTHON')).toBe('    ');
    expect(indentForNewLine('line one\n  if x:', 'PYTHON')).toBe('      ');
    expect(indentForNewLine('plain', 'JAVA')).toBe('');
  });
});
