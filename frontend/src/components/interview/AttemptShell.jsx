import { Badge, Button, ErrorMessage } from '../ui.jsx';
import ConfirmDialog from '../mocktest/ConfirmDialog.jsx';
import { formatDuration } from '../../utils/format.js';
import { levelLabel, levelTone, progressLabel } from '../../utils/interview.js';
import { progressPercent } from '../../utils/mockTests.js';

/**
 * Frame shared by every assessment: title, role, round and level, timer, progress bar, save status, tab notice,
 * errors and the submit confirmation. The round-specific body is passed as children.
 */
export default function AttemptShell({ session, answeredCount, summaryLines, confirming, setConfirming, children }) {
  const { attempt, remaining, saveState, error, submitting, submit, tabSwitches } = session;
  const saveText = { saved: 'All changes saved', saving: 'Saving...', error: 'Could not save, will retry', unsaved: 'Unsaved changes' }[saveState];

  return (
    <div className="cf-content">
      <div className="cf-card" style={{ marginBottom: 16 }}>
        <div className="cf-attempt-head">
          <div>
            <strong>{attempt.roundLabel}</strong>
            <div className="cf-test-card-badges" style={{ marginTop: 4 }}>
              <Badge tone="success">{attempt.roleName}</Badge>
              <Badge tone={levelTone(attempt.level)}>{levelLabel(attempt.level)}</Badge>
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className={`cf-timer ${remaining !== null && remaining < 60 ? 'low' : ''}`} aria-label="Time remaining">
              {remaining === null ? '--:--' : formatDuration(remaining)}
            </div>
            <small className="cf-muted">Time remaining</small>
          </div>
        </div>
        <div className="cf-progress" style={{ marginTop: 12 }} role="progressbar" aria-label="Progress"
          aria-valuemin={0} aria-valuemax={attempt.totalQuestions} aria-valuenow={answeredCount}>
          <div style={{ width: `${progressPercent(answeredCount, attempt.totalQuestions)}%` }} />
        </div>
        <div className="cf-muted" style={{ marginTop: 6 }}>{progressLabel(attempt.round, answeredCount, attempt.totalQuestions)} &middot; {saveText}</div>
      </div>

      {tabSwitches > 0 && <div className="cf-alert cf-alert-warn">You left this tab {tabSwitches} time(s). Please stay on the assessment page.</div>}
      <ErrorMessage message={error} />

      {children}

      <ConfirmDialog open={confirming} title="Submit your attempt?" confirmLabel="Submit" cancelLabel="Keep working" busy={submitting}
        onCancel={() => setConfirming(false)} onConfirm={() => { setConfirming(false); submit(); }}>
        {summaryLines}
        <p className="cf-muted">You cannot change your answers after submitting.</p>
      </ConfirmDialog>
    </div>
  );
}
