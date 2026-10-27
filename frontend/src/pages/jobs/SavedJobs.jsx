import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import { Badge, EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { formatDate, labelize } from '../../utils/format.js';
import JobCard from './JobCard.jsx';

export default function SavedJobs() {
  const [jobs, setJobs] = useState(null);
  const [apps, setApps] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.get('/jobs/saved'), api.get('/applications')])
      .then(([j, a]) => { setJobs(j.data); setApps(a.data); })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const unsave = async (job) => {
    try {
      await api.delete(`/jobs/${job.id}/save`);
      setJobs((list) => list.filter((j) => j.id !== job.id));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  if (error) return <ErrorMessage message={error} />;
  if (!jobs || !apps) return <LoadingSpinner />;

  return (
    <>
      <PageHeader title="Saved jobs & applications" />
      <section className="cf-section">
        <h2>Saved jobs</h2>
        {jobs.length === 0
          ? <EmptyState title="No saved jobs" text="Save jobs you like and they will show up here." actionLabel="Browse jobs" actionTo="/job-search" />
          : <div className="cf-grid">{jobs.map((j) => <JobCard key={j.id} job={{ ...j, saved: true }} base="/job-search" onToggleSave={unsave} />)}</div>}
      </section>
      <section className="cf-section">
        <h2>Application tracker</h2>
        {apps.length === 0 ? <p className="cf-muted">Open a job and use “Track application” to record where you applied.</p> : (
          <div className="cf-card cf-table-wrap">
            <table className="cf-table">
              <thead><tr><th>Job</th><th>Company</th><th>Status</th><th>Notes</th><th>Updated</th></tr></thead>
              <tbody>
                {apps.map((a) => (
                  <tr key={a.jobId}>
                    <td><Link to={`/job-search/${a.jobId}`}>{a.jobTitle}</Link></td>
                    <td>{a.company}</td>
                    <td><Badge>{labelize(a.status)}</Badge></td>
                    <td className="cf-muted">{a.notes || '-'}</td>
                    <td>{formatDate(a.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
