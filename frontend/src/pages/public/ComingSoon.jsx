import { Link } from 'react-router-dom';
import { EmptyState } from '../../components/ui.jsx';

export default function ComingSoon({ title, phase }) {
  return (
    <div className="cf-content">
      <h1>{title}</h1>
      <EmptyState
        title={phase ? 'Not implemented yet' : 'Nothing here'}
        text={phase ? `${title} is planned for ${phase} of the project and is not available in this build.` : 'The page you are looking for does not exist.'}
        actionLabel="Go to dashboard"
        actionTo="/dashboard"
      />
      <p className="cf-muted" style={{ textAlign: 'center', marginTop: 12 }}><Link to="/">Home</Link></p>
    </div>
  );
}
