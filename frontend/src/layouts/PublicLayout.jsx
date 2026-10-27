import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function PublicLayout() {
  const { user } = useAuth();
  return (
    <>
      <header className="cf-public-nav">
        <Link className="cf-brand" style={{ color: 'var(--cf-primary)', padding: 0 }} to="/">CareerFit <span style={{ color: 'var(--cf-accent)' }}>AI</span></Link>
        <nav>
          <Link to="/features">Features</Link>
          <Link to="/about">About</Link>
          <Link to="/jobs">Jobs</Link>
          {user
            ? <Link className="cf-btn cf-btn-primary" to="/dashboard">Dashboard</Link>
            : <>
                <Link to="/login">Log in</Link>
                <Link className="cf-btn cf-btn-primary" to="/register">Get started</Link>
              </>}
        </nav>
      </header>
      <Outlet />
    </>
  );
}
