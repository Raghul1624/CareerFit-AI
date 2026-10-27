import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import useInterviewSession from '../../hooks/useInterviewSession.js';
import { ErrorMessage, LoadingSpinner } from '../../components/ui.jsx';
import AttemptShell from '../../components/interview/AttemptShell.jsx';
import McqAssessment from '../../components/interview/McqAssessment.jsx';
import TextAssessment from '../../components/interview/TextAssessment.jsx';
import CodingAssessment from '../../components/interview/CodingAssessment.jsx';
import { answeredTotal, markedTotal } from '../../utils/interview.js';

/** A running assessment. It uses its own focused layout (no sidebar), like the mock-test page. */
export default function InterviewAttempt() {
  const { id } = useParams();
  const session = useInterviewSession(id);
  const [index, setIndex] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const { attempt, answers, update, submitting, setError } = session;

  if (session.error && !attempt) {
    return (
      <div className="cf-content">
        <ErrorMessage message={session.error} />
        <p><Link to="/interview">Back to Interview Preparation</Link></p>
      </div>
    );
  }
  if (!attempt) return <div className="cf-content"><LoadingSpinner label="Loading your assessment..." /></div>;

  const answered = answeredTotal(attempt.questions, answers);
  const marked = markedTotal(attempt.questions, answers);
  const unanswered = attempt.totalQuestions - answered;
  const isCoding = attempt.round === 'CODING';
  const summaryLines = (
    <>
      <p>{isCoding ? 'You have worked on' : 'You have answered'} <strong>{answered}</strong> of <strong>{attempt.totalQuestions}</strong> {isCoding ? 'problems' : 'questions'}.</p>
      {unanswered > 0 && <p className="cf-alert cf-alert-warn">{unanswered} {isCoding ? 'problem(s) have no submitted code and will score 0' : 'question(s) are unanswered and will score 0'}.</p>}
      {isCoding && <p className="cf-muted">Only code you SUBMITTED is scored. Press Submit code on each problem first.</p>}
      {marked > 0 && <p>{marked} item(s) are still marked for review.</p>}
    </>
  );

  const bodyProps = { attempt, answers, update, index, setIndex, onSubmitClick: () => setConfirming(true), submitting, setError };
  let body;
  if (attempt.round === 'CODING') body = <CodingAssessment {...bodyProps} />;
  else if (attempt.round === 'TECHNICAL_INTERVIEW' || attempt.round === 'HR_INTERVIEW') body = <TextAssessment {...bodyProps} />;
  else body = <McqAssessment {...bodyProps} />;

  return (
    <AttemptShell session={session} answeredCount={answered} summaryLines={summaryLines} confirming={confirming} setConfirming={setConfirming}>
      {body}
    </AttemptShell>
  );
}
