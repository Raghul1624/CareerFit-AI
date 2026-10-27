import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { Button, EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import JobCard from './JobCard.jsx';

const empty = { q: '', location: '', workMode: '', experienceLevel: '', employmentType: '' };

/** mode: 'public' (no login), 'browse' (all jobs) or 'matches' (ranked by match %). */
export default function JobList({ mode }) {
  const isPublic = mode === 'public';
  const isMatches = mode === 'matches';
  const base = isPublic ? '/jobs' : '/job-search';
  const [form, setForm] = useState(empty);
  const [filters, setFilters] = useState(empty);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setData(null);
    setError('');
    const url = isPublic ? '/public/jobs' : isMatches ? '/jobs/matches' : '/jobs';
    const params = { page, size: 8 };
    if (!isMatches) Object.entries(filters).forEach(([k, v]) => { if (v) params[k] = v; });
    api.get(url, { params }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [isPublic, isMatches, filters, page]);

  useEffect(load, [load]);

  const toggleSave = async (job) => {
    try {
      if (job.saved) await api.delete(`/jobs/${job.id}/save`);
      else await api.post(`/jobs/${job.id}/save`);
      setData((d) => ({ ...d, content: d.content.map((j) => (j.id === job.id ? { ...j, saved: !j.saved } : j)) }));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const apply = (e) => { e.preventDefault(); setPage(0); setFilters(form); };
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const title = isMatches ? 'Job matches' : isPublic ? 'Browse jobs' : 'Browse jobs';
  const subtitle = isMatches
    ? 'Ranked by how many of the job’s skills you have (declared, or 70%+ in tests).'
    : 'Listings are demo/platform entries unless marked otherwise; they are not live vacancies.';

  return (
    <div className={isPublic ? 'cf-content' : undefined}>
      <PageHeader title={title} subtitle={subtitle} />
      {!isMatches && (
        <form className="cf-card" onSubmit={apply} style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
          <input className="cf-input" placeholder="Title or company" value={form.q} onChange={set('q')} aria-label="Search" />
          <input className="cf-input" placeholder="Location" value={form.location} onChange={set('location')} aria-label="Location" />
          <select className="cf-input" value={form.workMode} onChange={set('workMode')} aria-label="Work mode">
            <option value="">Any work mode</option><option value="REMOTE">Remote</option><option value="HYBRID">Hybrid</option><option value="ONSITE">Onsite</option>
          </select>
          <select className="cf-input" value={form.experienceLevel} onChange={set('experienceLevel')} aria-label="Experience level">
            <option value="">Any experience</option><option value="FRESHER">Fresher</option><option value="JUNIOR">Junior</option><option value="MID">Mid</option><option value="SENIOR">Senior</option>
          </select>
          <select className="cf-input" value={form.employmentType} onChange={set('employmentType')} aria-label="Employment type">
            <option value="">Any type</option><option value="FULL_TIME">Full time</option><option value="PART_TIME">Part time</option><option value="INTERNSHIP">Internship</option><option value="CONTRACT">Contract</option>
          </select>
          <Button type="submit">Search</Button>
          <Button type="button" variant="outline" onClick={() => { setForm(empty); setFilters(empty); setPage(0); }}>Reset</Button>
        </form>
      )}
      <ErrorMessage message={error} />
      {!data && !error && <LoadingSpinner />}
      {data && data.content.length === 0 && <EmptyState title="No jobs found" text="Try changing or clearing the filters." />}
      {data && data.content.length > 0 && (
        <>
          <div className="cf-grid">
            {data.content.map((j) => <JobCard key={j.id} job={j} base={base} onToggleSave={isPublic ? null : toggleSave} />)}
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16, alignItems: 'center' }}>
            <span className="cf-muted">Page {data.page + 1} of {Math.max(data.totalPages, 1)} &middot; {data.totalElements} jobs</span>
            <Button variant="outline" onClick={() => setPage(page - 1)} disabled={page === 0}>Previous</Button>
            <Button variant="outline" onClick={() => setPage(page + 1)} disabled={page + 1 >= data.totalPages}>Next</Button>
          </div>
        </>
      )}
    </div>
  );
}
