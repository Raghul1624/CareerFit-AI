import { useEffect, useRef, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { Badge, Button, EmptyState, ErrorMessage, LoadingSpinner, PageHeader, SuccessMessage } from '../../components/ui.jsx';
import { formatDate } from '../../utils/format.js';

const MAX_BYTES = 2 * 1024 * 1024;

export default function ResumeAnalyzer() {
  const [resumes, setResumes] = useState(null);
  const [paths, setPaths] = useState([]);
  const [careerId, setCareerId] = useState('');
  const [analysis, setAnalysis] = useState(null);
  const [activeId, setActiveId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState('');
  const fileInput = useRef(null);

  const refresh = () => api.get('/resumes').then((r) => setResumes(r.data));

  useEffect(() => {
    Promise.all([api.get('/resumes'), api.get('/career-paths'), api.get('/profile')])
      .then(([r, c, p]) => {
        setResumes(r.data);
        setPaths(c.data);
        setCareerId(p.data.targetCareerPathId ? String(p.data.targetCareerPathId) : '');
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const upload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    setMessage('');
    // Client-side checks are only a convenience; the server validates everything again.
    if (!file.name.toLowerCase().endsWith('.pdf')) { setError('Please choose a PDF file.'); return; }
    if (file.size > MAX_BYTES) { setError('The file is larger than 2 MB.'); return; }
    setBusy('upload');
    try {
      const form = new FormData();
      form.append('file', file);
      await api.post('/resumes', form);
      await refresh();
      setMessage('Resume uploaded.');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy('');
      if (fileInput.current) fileInput.current.value = '';
    }
  };

  const analyze = async (id) => {
    setError('');
    setMessage('');
    setBusy(`analyze-${id}`);
    try {
      const { data } = await api.post(`/resumes/${id}/analyze`, careerId ? { careerPathId: Number(careerId) } : {});
      setAnalysis(data);
      setActiveId(id);
      await refresh();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy('');
    }
  };

  const remove = async (id) => {
    if (!window.confirm('Delete this resume and its analyses permanently?')) return;
    setError('');
    try {
      await api.delete(`/resumes/${id}`);
      if (activeId === id) { setAnalysis(null); setActiveId(null); }
      await refresh();
      setMessage('Resume deleted.');
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (!resumes && !error) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title="Resume analyzer" subtitle="Upload a PDF resume to see which skills are detected and what to improve." />
      <ErrorMessage message={error} />
      <SuccessMessage message={message} />

      <div className="cf-card" style={{ marginBottom: 16 }}>
        <h2>Upload a resume</h2>
        <p className="cf-muted">PDF only, up to 2 MB and 10 pages, text-based (not a scan). Your file is stored privately, visible only to you, until you delete it. The text is not stored, only the analysis result.</p>
        <input ref={fileInput} type="file" accept="application/pdf,.pdf" onChange={upload} disabled={busy === 'upload'} aria-label="Resume PDF" />
        {busy === 'upload' && <span className="cf-muted"> Uploading...</span>}
      </div>

      <div className="cf-card" style={{ marginBottom: 16 }}>
        <div className="cf-field" style={{ maxWidth: 340 }}>
          <label htmlFor="career">Compare against career</label>
          <select id="career" className="cf-input" value={careerId} onChange={(e) => setCareerId(e.target.value)}>
            <option value="">None (just detect skills)</option>
            {paths.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        {resumes && resumes.length === 0 ? (
          <EmptyState title="No resumes yet" text="Upload a PDF above to get started." />
        ) : (
          <div className="cf-table-wrap">
            <table className="cf-table">
              <thead><tr><th>File</th><th>Uploaded</th><th>Last coverage</th><th /></tr></thead>
              <tbody>
                {resumes?.map((r) => (
                  <tr key={r.id}>
                    <td>{r.filename} <span className="cf-muted">({Math.round(r.sizeBytes / 1024)} KB)</span></td>
                    <td>{formatDate(r.uploadedAt)}</td>
                    <td>{r.lastCoverage == null ? '-' : `${r.lastCoverage}%`}</td>
                    <td style={{ display: 'flex', gap: 6 }}>
                      <Button onClick={() => analyze(r.id)} loading={busy === `analyze-${r.id}`}>Analyze</Button>
                      <Button variant="outline" onClick={() => remove(r.id)}>Delete</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {analysis && (
        <div className="cf-two-col">
          <div className="cf-card">
            <h2>Keyword coverage{analysis.careerPathName ? ` for ${analysis.careerPathName}` : ''}</h2>
            {analysis.keywordCoverage == null ? (
              <p className="cf-muted">Choose a career above to compute coverage.</p>
            ) : (
              <>
                <strong style={{ fontSize: '1.8rem', color: 'var(--cf-primary)' }}>{analysis.keywordCoverage}%</strong>
                <div className="cf-progress"><div style={{ width: `${analysis.keywordCoverage}%` }} /></div>
              </>
            )}
            <p className="cf-muted" style={{ marginTop: 8, fontSize: '0.85rem' }}>{analysis.disclaimer}</p>
            <h3>Skills detected</h3>
            <div className="cf-pill-list">
              {analysis.extractedSkills.length === 0 ? <span className="cf-muted">None detected</span>
                : analysis.extractedSkills.map((s) => <Badge key={s} tone="success">{s}</Badge>)}
            </div>
            {analysis.missingSkills.length > 0 && <>
              <h3 style={{ marginTop: 14 }}>Expected but not found</h3>
              <div className="cf-pill-list">{analysis.missingSkills.map((s) => <Badge key={s} tone="danger">{s}</Badge>)}</div>
            </>}
            <p className="cf-muted" style={{ marginTop: 12 }}>{analysis.wordCount} words &middot; {analysis.pageCount} page(s)</p>
          </div>
          <div className="cf-card">
            <h2>Suggestions</h2>
            {analysis.suggestions.length === 0
              ? <p className="cf-muted">No rule-based suggestions. Nice work.</p>
              : <ul>{analysis.suggestions.map((s) => <li key={s} style={{ marginBottom: 8 }}>{s}</li>)}</ul>}
            {analysis.aiFeedback && analysis.aiFeedback.length > 0 && <>
              <h3>AI feedback (optional)</h3>
              <ul>{analysis.aiFeedback.map((s) => <li key={s}>{s}</li>)}</ul>
            </>}
          </div>
        </div>
      )}
    </>
  );
}
