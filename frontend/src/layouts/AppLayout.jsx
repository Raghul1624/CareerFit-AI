import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { BarChart3, BookOpen, Mic, Bookmark, Briefcase, Search, FileText, History, LayoutDashboard, LogOut, Map, Menu, Route, Shield, User } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';

const links = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/career-paths', label: 'Career Paths', icon: Route },
  { to: '/tests', label: 'Mock Tests', icon: BookOpen },
  { to: '/history', label: 'Test History', icon: History },
  { to: '/interview', label: 'Interview Prep', icon: Mic },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/resume', label: 'Resume Analyzer', icon: FileText },
  { to: '/job-matches', label: 'Job Matches', icon: Briefcase },
  { to: '/job-search', label: 'Browse Jobs', icon: Search },
  { to: '/saved-jobs', label: 'Saved Jobs', icon: Bookmark },
  { to: '/roadmap', label: 'Learning Roadmap', icon: Map },
  { to: '/profile', label: 'Profile', icon: User },
];

export default function AppLayout() {
  const { user, logout, isAdmin } = useAuth();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="cf-shell">
      <aside className={`cf-sidebar ${open ? 'open' : ''}`}>
        <Link className="cf-brand" to="/dashboard">CareerFit <span>AI</span></Link>
        <nav className="cf-nav" onClick={() => setOpen(false)}>
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to}><Icon size={17} /> {label}</NavLink>
          ))}
          {isAdmin && <NavLink to="/admin"><Shield size={17} /> Admin</NavLink>}
        </nav>
      </aside>
      <div className="cf-main">
        <header className="cf-topbar">
          <button className="cf-btn cf-btn-outline cf-menu-btn" onClick={() => setOpen(!open)} aria-label="Toggle navigation">
            <Menu size={18} />
          </button>
          <span className="cf-muted">Signed in as <strong>{user?.fullName}</strong></span>
          <button className="cf-btn cf-btn-outline cf-btn-sm" onClick={handleLogout}><LogOut size={15} /> Log out</button>
        </header>
        <main className="cf-content"><Outlet /></main>
      </div>
    </div>
  );
}
