import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client.js';
import { getRole } from '../../api/interview.js';
import { Badge, ErrorMessage, LoadingSpinner, PageHeader } from '../../components/ui.jsx';
import { levelLabel, levelTone, roundAvailable } from '../../utils/interview.js';

/** The five rounds of one role. A round is only selectable when at least one difficulty level has enough questions. */
export default function InterviewRole() {
  const { roleCode } = useParams();
  const [role, setRole] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setRole(null);
    getRole(roleCode).then(setRole).catch((e) => setError(errorMessage(e)));
  }, [roleCode]);

  if (error) return <><ErrorMessage message={error} /><p><Link to="/interview">Back to roles</Link></p></>;
  if (!role) return <LoadingSpinner />;

  return (
    <>
      <p><Link to="/interview">&larr; All roles</Link></p>
      <PageHeader title={role.name} subtitle={role.description} />
      <div className="cf-grid cf-round-grid">
        {role.rounds.map((round, i) => (
          <div key={round.round} className="cf-card cf-round-card">
            <div className="cf-round-number">Round {i + 1}</div>
            <h3>{round.label}</h3>
            <p className="cf-muted">{round.description}</p>
            <div className="cf-pill-list">
              {round.levels.map((l) => (
                <Badge key={l.level} tone={l.available ? levelTone(l.level) : 'neutral'}>
                  {levelLabel(l.level)}{l.available ? '' : ' (not available)'}
                </Badge>
              ))}
            </div>
            {roundAvailable(round)
              ? <Link className="cf-btn cf-btn-primary cf-btn-sm" to={`/interview/${role.code}/${round.round}`}>Select round</Link>
              : <p className="cf-muted" role="note">{round.levels[0]?.reason || 'Not available yet.'}</p>}
          </div>
        ))}
      </div>
    </>
  );
}
