import { describe, expect, it } from 'vitest';
import { formatDuration, formatPercent, labelize } from './format.js';

describe('format helpers', () => {
  it('formats seconds as mm:ss', () => {
    expect(formatDuration(65)).toBe('01:05');
    expect(formatDuration(0)).toBe('00:00');
    expect(formatDuration(-5)).toBe('00:00');
  });
  it('formats percentages and missing values', () => {
    expect(formatPercent(66.666)).toBe('66.7%');
    expect(formatPercent(null)).toBe('-');
  });
  it('labelizes enum values', () => {
    expect(labelize('FULL_TIME')).toBe('Full time');
    expect(labelize(null)).toBe('');
  });
});
