import { Link } from 'react-router-dom';
import { Bookmark, BookmarkCheck } from 'lucide-react';
import { Badge } from '../../components/ui.jsx';
import { labelize } from '../../utils/format.js';

export default function JobCard({ job, base, onToggleSave }) {
  return (
    <div className="cf-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
        <div>
          <h3 style={{ marginBottom: 2 }}><Link to={`${base}/${job.id}`}>{job.title}</Link></h3>
          <div className="cf-muted">{job.company} &middot; {job.location}</div>
        </div>
        {onToggleSave && (
          <button className="cf-btn cf-btn-outline cf-btn-sm" onClick={() => onToggleSave(job)}
            aria-label={job.saved ? 'Remove from saved jobs' : 'Save job'}>
            {job.saved ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
          </button>
        )}
      </div>
      <div className="cf-pill-list" style={{ margin: '10px 0' }}>
        {job.sample && <Badge tone="warning">Sample listing</Badge>}
        {job.workMode && <Badge>{labelize(job.workMode)}</Badge>}
        {job.experienceLevel && <Badge>{labelize(job.experienceLevel)}</Badge>}
        {job.employmentType && <Badge>{labelize(job.employmentType)}</Badge>}
        {job.matchPercent != null && <Badge tone={job.matchPercent >= 60 ? 'success' : 'neutral'}>{job.matchPercent}% match</Badge>}
      </div>
      <div className="cf-muted" style={{ fontSize: '0.85rem' }}>Required: {job.requiredSkills.join(', ') || '-'}</div>
    </div>
  );
}
