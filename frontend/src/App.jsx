import { Route, Routes } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute.jsx';
import AdminRoute from './routes/AdminRoute.jsx';
import AppLayout from './layouts/AppLayout.jsx';
import PublicLayout from './layouts/PublicLayout.jsx';
import Landing from './pages/public/Landing.jsx';
import InfoPage from './pages/public/InfoPage.jsx';
import ComingSoon from './pages/public/ComingSoon.jsx';
import Login from './pages/auth/Login.jsx';
import Register from './pages/auth/Register.jsx';
import Dashboard from './pages/dashboard/Dashboard.jsx';
import CareerPaths from './pages/careers/CareerPaths.jsx';
import CareerPathDetails from './pages/careers/CareerPathDetails.jsx';
import MockTests from './pages/mocktests/MockTests.jsx';
import TestInstructions from './pages/mocktests/TestInstructions.jsx';
import ActiveTest from './pages/mocktests/ActiveTest.jsx';
import TestResult from './pages/results/TestResult.jsx';
import TestHistory from './pages/results/TestHistory.jsx';
import Profile from './pages/profile/Profile.jsx';
import JobList from './pages/jobs/JobList.jsx';
import JobDetails from './pages/jobs/JobDetails.jsx';
import SavedJobs from './pages/jobs/SavedJobs.jsx';
import ResumeAnalyzer from './pages/resume/ResumeAnalyzer.jsx';
import Analytics from './pages/analytics/Analytics.jsx';
import LearningRoadmaps from './pages/learning/LearningRoadmaps.jsx';
import RoadmapDetail from './pages/learning/RoadmapDetail.jsx';
import InterviewHome from './pages/interview/InterviewHome.jsx';
import InterviewRole from './pages/interview/InterviewRole.jsx';
import InterviewSetup from './pages/interview/InterviewSetup.jsx';
import InterviewAttempt from './pages/interview/InterviewAttempt.jsx';
import InterviewResult from './pages/interview/InterviewResult.jsx';
import InterviewHistory from './pages/interview/InterviewHistory.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Landing />} />
        <Route path="/about" element={<InfoPage kind="about" />} />
        <Route path="/features" element={<InfoPage kind="features" />} />
        <Route path="/jobs" element={<JobList mode="public" />} />
        <Route path="/jobs/:id" element={<JobDetails publicMode />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        {/* The active test uses its own focused layout (no sidebar). */}
        <Route path="/attempts/:id/take" element={<ActiveTest />} />
        <Route path="/interview/attempts/:id" element={<InterviewAttempt />} />
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/career-paths" element={<CareerPaths />} />
          <Route path="/career-paths/:id" element={<CareerPathDetails />} />
          <Route path="/tests" element={<MockTests />} />
          <Route path="/tests/:id" element={<TestInstructions />} />
          <Route path="/results/:id" element={<TestResult />} />
          <Route path="/history" element={<TestHistory />} />
          <Route path="/interview" element={<InterviewHome />} />
          <Route path="/interview/history" element={<InterviewHistory />} />
          <Route path="/interview/attempts/:id/result" element={<InterviewResult />} />
          <Route path="/interview/:roleCode" element={<InterviewRole />} />
          <Route path="/interview/:roleCode/:round" element={<InterviewSetup />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/resume" element={<ResumeAnalyzer />} />
          <Route path="/job-matches" element={<JobList mode="matches" />} />
          <Route path="/job-search" element={<JobList mode="browse" />} />
          <Route path="/job-search/:id" element={<JobDetails />} />
          <Route path="/saved-jobs" element={<SavedJobs />} />
          <Route path="/roadmap" element={<LearningRoadmaps />} />
          <Route path="/roadmap/:id" element={<RoadmapDetail />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<ComingSoon title="Admin Dashboard" phase="Phase 9" />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<ComingSoon title="Page not found" />} />
    </Routes>
  );
}
