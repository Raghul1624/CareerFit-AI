import { useMemo, useRef } from 'react';
import { highlightCode, indentForNewLine } from '../../utils/codeHighlight.js';

/**
 * A dependency-free code editor: a monospace textarea over a syntax-highlighted layer, with line numbers,
 * Tab to indent, Enter to keep the indentation and Shift+Tab to outdent. The highlighted HTML is built from
 * escaped text only (see utils/codeHighlight.js).
 */
export default function CodeEditor({ value, language, onChange, disabled = false, label = 'Code editor' }) {
  const textRef = useRef(null);
  const preRef = useRef(null);
  const gutterRef = useRef(null);
  const html = useMemo(() => highlightCode(value, language) + '\n', [value, language]);
  const lineCount = useMemo(() => value.split('\n').length, [value]);

  const syncScroll = () => {
    const t = textRef.current;
    if (!t) return;
    if (preRef.current) { preRef.current.scrollTop = t.scrollTop; preRef.current.scrollLeft = t.scrollLeft; }
    if (gutterRef.current) gutterRef.current.scrollTop = t.scrollTop;
  };

  const replaceRange = (start, end, insert, caret) => {
    const next = value.slice(0, start) + insert + value.slice(end);
    onChange(next);
    requestAnimationFrame(() => {
      if (textRef.current) textRef.current.setSelectionRange(caret, caret);
    });
  };

  const onKeyDown = (e) => {
    const t = e.target;
    const { selectionStart: start, selectionEnd: end } = t;
    if (e.key === 'Tab') {
      e.preventDefault();
      if (e.shiftKey) {
        const lineStart = value.lastIndexOf('\n', start - 1) + 1;
        const m = value.slice(lineStart).match(/^( {1,4}|\t)/);
        if (m) replaceRange(lineStart, lineStart + m[0].length, '', Math.max(lineStart, start - m[0].length));
      } else {
        replaceRange(start, end, '    ', start + 4);
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const insert = '\n' + indentForNewLine(value.slice(0, start), language);
      replaceRange(start, end, insert, start + insert.length);
    }
  };

  return (
    <div className="cf-editor">
      <div className="cf-editor-gutter" ref={gutterRef} aria-hidden="true">
        {Array.from({ length: lineCount }, (_, i) => <div key={i}>{i + 1}</div>)}
      </div>
      <div className="cf-editor-body">
        <pre className="cf-editor-highlight" ref={preRef} aria-hidden="true" dangerouslySetInnerHTML={{ __html: html }} />
        <textarea ref={textRef} className="cf-editor-input" value={value} spellCheck={false} wrap="off" disabled={disabled}
          aria-label={label} autoCapitalize="off" autoComplete="off" autoCorrect="off"
          onChange={(e) => onChange(e.target.value)} onKeyDown={onKeyDown} onScroll={syncScroll} />
      </div>
    </div>
  );
}
