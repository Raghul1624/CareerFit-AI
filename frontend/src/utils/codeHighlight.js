/**
 * Tiny syntax highlighter for the code editor. It returns HTML in which EVERY piece of user text is escaped, so it is
 * safe to inject with dangerouslySetInnerHTML. It is deliberately simple (no parser): comments, strings, numbers and keywords.
 */
const KEYWORDS = {
  JAVA: 'abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for if implements import instanceof int interface long new package private protected public return short static super switch this throw throws try var void volatile while true false null String List Map Set ArrayList HashMap Scanner System',
  PYTHON: 'and as assert break class continue def del elif else except finally for from global if import in is lambda None nonlocal not or pass raise return try while with yield True False print input range len int str float list dict set sorted',
  JAVASCRIPT: 'async await break case catch class const continue default delete do else export extends finally for function if import in instanceof let new null of return super switch this throw try typeof undefined var void while yield true false console require',
};

const COMMENT = { JAVA: '\\/\\/[^\\n]*|\\/\\*[\\s\\S]*?\\*\\/', JAVASCRIPT: '\\/\\/[^\\n]*|\\/\\*[\\s\\S]*?\\*\\/', PYTHON: '#[^\\n]*' };

export function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

export function highlightCode(code, language) {
  const lang = KEYWORDS[language] ? language : 'JAVA';
  const keywords = KEYWORDS[lang].split(' ').join('|');
  const strings = lang === 'PYTHON'
    ? '"""[\\s\\S]*?"""|\'\'\'[\\s\\S]*?\'\'\'|"(?:\\\\.|[^"\\\\\\n])*"|\'(?:\\\\.|[^\'\\\\\\n])*\''
    : '"(?:\\\\.|[^"\\\\\\n])*"|\'(?:\\\\.|[^\'\\\\\\n])*\'' + (lang === 'JAVASCRIPT' ? '|`(?:\\\\.|[^`\\\\])*`' : '');
  const re = new RegExp(`(${COMMENT[lang]})|(${strings})|(\\b\\d+(?:\\.\\d+)?\\b)|(\\b(?:${keywords})\\b)`, 'g');
  let out = '';
  let last = 0;
  let m = re.exec(code);
  while (m !== null) {
    out += escapeHtml(code.slice(last, m.index));
    const cls = m[1] ? 'cm-comment' : m[2] ? 'cm-string' : m[3] ? 'cm-number' : 'cm-keyword';
    out += `<span class="${cls}">${escapeHtml(m[0])}</span>`;
    last = m.index + m[0].length;
    if (m[0].length === 0) re.lastIndex += 1;
    m = re.exec(code);
  }
  return out + escapeHtml(code.slice(last));
}

/** Auto-indent for the Enter key: keep the current line's indentation and add one level after an opening brace or colon. */
export function indentForNewLine(textBeforeCursor, language) {
  const lineStart = textBeforeCursor.lastIndexOf('\n') + 1;
  const line = textBeforeCursor.slice(lineStart);
  const indent = (line.match(/^[ \t]*/) || [''])[0];
  const trimmed = line.trimEnd();
  const opens = language === 'PYTHON' ? trimmed.endsWith(':') : /[{(\[]$/.test(trimmed);
  return indent + (opens ? '    ' : '');
}
