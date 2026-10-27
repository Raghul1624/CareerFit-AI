import { useEffect, useState } from 'react';
import { errorMessage } from '../../api/client.js';
import { getCodingStatus, listSubmissions, runCode, submitCode } from '../../api/interview.js';
import { Badge, Button, ErrorMessage } from '../ui.jsx';
import CodeEditor from './CodeEditor.jsx';
import QuestionPalette from '../mocktest/QuestionPalette.jsx';
import { paletteAnswers, verdictLabel, verdictTone } from '../../utils/interview.js';

const LANGUAGES = [{ value: 'JAVA', label: 'Java' }, { value: 'PYTHON', label: 'Python' }, { value: 'JAVASCRIPT', label: 'JavaScript' }];

function CaseCard({ c }) {
  return (
    <div className={`cf-case ${c.verdict === 'PASSED' ? 'ok' : 'bad'}`}>
      <div className="cf-case-head">
        <strong>{c.hidden ? `Hidden test ${c.index}` : `Sample test ${c.index}`}</strong>
        <Badge tone={verdictTone(c.verdict)}>{verdictLabel(c.verdict)}</Badge>
        {c.timeMs != null && <small className="cf-muted">{c.timeMs} ms</small>}
      </div>
      {c.hidden ? <small className="cf-muted">Hidden tests only report passed or failed.</small> : (
        <>
          <div className="cf-io"><span>Input</span><pre>{c.input}</pre></div>
          <div className="cf-io"><span>Expected output</span><pre>{c.expectedOutput}</pre></div>
          <div className="cf-io"><span>Your output</span><pre>{c.actualOutput || '(no output)'}</pre></div>
          {c.errorOutput && <div className="cf-io"><span>Error</span><pre className="cf-error-text">{c.errorOutput}</pre></div>}
        </>
      )}
    </div>
  );
}

/**
 * Coding workspace: problem statement, editor with Java / Python / JavaScript, Run (samples or custom input)
 * and Submit (all samples plus hidden tests). Code is executed by the server's external sandbox, never in the browser.
 */
export default function CodingAssessment({ attempt, answers, update, index, setIndex, onSubmitClick, submitting, setError }) {
  const q = attempt.questions[index];
  const coding = q.coding;
  const a = answers[q.id] || {};
  const language = a.language || 'PYTHON';
  const starter = coding.starterCode[language];
  const code = a.text && a.language === language ? a.text : (drafts(a)[language] ?? starter);

  const [status, setStatus] = useState(null);
  const [useCustom, setUseCustom] = useState(false);
  const [customInput, setCustomInput] = useState('');
  const [busy, setBusy] = useState('');                 // '', 'run' or 'submit'
  const [runResult, setRunResult] = useState(null);
  const [submission, setSubmission] = useState(null);
  const [history, setHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);
  const [localError, setLocalError] = useState('');

  useEffect(() => { getCodingStatus().then(setStatus).catch(() => setStatus({ configured: false, message: 'Could not check the code execution service.' })); }, []);
  useEffect(() => { setRunResult(null); setSubmission(null); setHistory([]); setShowHistory(false); setLocalError(''); }, [q.id]);

  // Remember each language's draft separately so switching languages never loses code.
  function drafts(answer) { return answer.drafts || {}; }
  const setCode = (value) => update(q.id, { text: value, language, drafts: { ...drafts(a), [language]: value }, starter });
  const switchLanguage = (next) => {
    const nextCode = drafts(a)[next] ?? coding.starterCode[next];
    update(q.id, { language: next, text: nextCode, drafts: { ...drafts(a), [language]: code }, starter: coding.starterCode[next] });
  };
  const reset = () => setCode(starter);

  const disabled = !status?.configured;
  const run = async () => {
    setBusy('run'); setLocalError(''); setSubmission(null);
    try {
      const body = { language, sourceCode: code };
      if (useCustom && customInput.length > 0) body.stdin = customInput;
      setRunResult(await runCode(attempt.id, q.id, body));
    } catch (e) { setLocalError(errorMessage(e)); setRunResult(null); } finally { setBusy(''); }
  };
  const submit = async () => {
    setBusy('submit'); setLocalError(''); setRunResult(null);
    try {
      const result = await submitCode(attempt.id, q.id, { language, sourceCode: code });
      setSubmission(result);
      update(q.id, { submitted: true, best: Math.max(a.best ?? 0, result.scorePercent) });
      if (showHistory) setHistory(await listSubmissions(attempt.id, q.id));
    } catch (e) {
      setLocalError(errorMessage(e));
      if (e.response?.status === 409) setError('This attempt has ended.');
    } finally { setBusy(''); }
  };
  const toggleHistory = async () => {
    if (!showHistory) { try { setHistory(await listSubmissions(attempt.id, q.id)); } catch (e) { setLocalError(errorMessage(e)); } }
    setShowHistory(!showHistory);
  };

  const best = a.best ?? coding.bestScorePercent;

  return (
    <div className="cf-coding-layout">
      <div className="cf-card cf-problem">
        <div className="cf-test-card-badges" style={{ marginBottom: 6 }}>
          <Badge tone="success">Problem {index + 1} of {attempt.totalQuestions}</Badge>
          {best != null && <Badge tone={best >= 100 ? 'success' : 'warning'}>Best score {Math.round(best)}%</Badge>}
        </div>
        <h2>{q.title}</h2>
        <p style={{ whiteSpace: 'pre-wrap' }}>{q.text}</p>
        <h4>Input format</h4><p className="cf-muted" style={{ whiteSpace: 'pre-wrap' }}>{coding.inputFormat}</p>
        <h4>Output format</h4><p className="cf-muted" style={{ whiteSpace: 'pre-wrap' }}>{coding.outputFormat}</p>
        <h4>Constraints</h4><p className="cf-muted">{coding.constraints}</p>
        <h4>Sample test cases</h4>
        {coding.samples.map((s, i) => (
          <div key={i} className="cf-case">
            <div className="cf-io"><span>Input</span><pre>{s.input}</pre></div>
            <div className="cf-io"><span>Output</span><pre>{s.expectedOutput}</pre></div>
          </div>
        ))}
        <p className="cf-muted">Your code is also judged on {coding.hiddenTestCount} hidden test cases when you submit. Time limit {coding.timeLimitSeconds}s per test.</p>
        <div className="cf-actions-row">
          {attempt.questions.length > 1 && <QuestionPalette questions={attempt.questions} answers={paletteAnswers(attempt.questions, answers)} current={index} onSelect={setIndex} />}
        </div>
      </div>

      <div className="cf-card cf-workspace">
        {status && !status.configured && <div className="cf-alert cf-alert-warn" role="alert">{status.message}</div>}
        <div className="cf-toolbar">
          <select className="cf-input" aria-label="Language" value={language} onChange={(e) => switchLanguage(e.target.value)}>
            {LANGUAGES.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
          </select>
          <Button variant="outline" onClick={reset}>Reset code</Button>
          <label className="cf-check"><input type="checkbox" checked={useCustom} onChange={(e) => setUseCustom(e.target.checked)} /> Custom input</label>
        </div>
        <CodeEditor value={code} language={language} onChange={setCode} />
        {useCustom && (
          <textarea className="cf-input cf-mono" rows={3} placeholder="Type the standard input for your program..." aria-label="Custom input"
            value={customInput} onChange={(e) => setCustomInput(e.target.value)} />
        )}
        <div className="cf-actions-row">
          <Button variant="outline" onClick={run} loading={busy === 'run'} disabled={disabled || busy === 'submit'}>Run code</Button>
          <Button onClick={submit} loading={busy === 'submit'} disabled={disabled || busy === 'run'}>Submit code</Button>
          <Button variant="outline" onClick={toggleHistory}>{showHistory ? 'Hide' : 'Show'} my submissions</Button>
        </div>
        <ErrorMessage message={localError} />
        {busy && <p className="cf-muted">Running in the sandbox. This can take several seconds...</p>}

        {runResult && (
          <div className="cf-output" aria-live="polite">
            <h4>{runResult.custom ? 'Output' : `Sample tests: ${runResult.passed} of ${runResult.total} passed`}</h4>
            {runResult.custom ? (
              <>
                {runResult.status !== 'OK' && <Badge tone="danger">{verdictLabel(runResult.status)}</Badge>}
                {runResult.compileOutput && <pre className="cf-error-text">{runResult.compileOutput}</pre>}
                <pre>{runResult.stdout || '(no output)'}</pre>
                {runResult.stderr && <pre className="cf-error-text">{runResult.stderr}</pre>}
                {runResult.timeMs != null && <small className="cf-muted">{runResult.timeMs} ms</small>}
              </>
            ) : (
              <>
                {runResult.compileOutput && <><Badge tone="danger">Compilation error</Badge><pre className="cf-error-text">{runResult.compileOutput}</pre></>}
                {runResult.cases.map((c) => <CaseCard key={c.index} c={c} />)}
              </>
            )}
          </div>
        )}

        {submission && (
          <div className="cf-output" aria-live="polite">
            <h4>Submission result <Badge tone={verdictTone(submission.verdict)}>{verdictLabel(submission.verdict)}</Badge></h4>
            <p><strong>{submission.passedTests} of {submission.totalTests}</strong> test cases passed &middot; score {submission.scorePercent}%</p>
            {submission.compileOutput && <pre className="cf-error-text">{submission.compileOutput}</pre>}
            {submission.cases.map((c) => <CaseCard key={c.index} c={c} />)}
          </div>
        )}

        {showHistory && (
          <div className="cf-output">
            <h4>My submissions for this problem</h4>
            {history.length === 0 ? <p className="cf-muted">No submissions yet.</p> : (
              <table className="cf-table"><thead><tr><th>#</th><th>Language</th><th>Result</th><th>Tests</th><th>Score</th></tr></thead>
                <tbody>{history.map((s, i) => (
                  <tr key={s.id}><td>{history.length - i}</td><td>{s.language}</td><td><Badge tone={verdictTone(s.verdict)}>{verdictLabel(s.verdict)}</Badge></td>
                    <td>{s.passedTests}/{s.totalTests}</td><td>{s.scorePercent}%</td></tr>
                ))}</tbody></table>
            )}
          </div>
        )}

        <div className="cf-actions-row">
          <Button variant="outline" onClick={() => setIndex(index - 1)} disabled={index === 0}>Previous problem</Button>
          <Button variant="outline" onClick={() => setIndex(index + 1)} disabled={index === attempt.questions.length - 1}>Next problem</Button>
          <Button onClick={onSubmitClick} loading={submitting}>Finish and submit attempt</Button>
        </div>
      </div>
    </div>
  );
}
