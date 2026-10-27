import { Link } from 'react-router-dom';

const features = [
  ['Career paths', 'Browse roles such as Java Developer, Data Analyst and more, with the skills each one needs.'],
  ['Mock tests', 'Timed skill tests and full career mock tests with server-side scoring and explanations.'],
  ['Progress tracking', 'Review past attempts and see skill-wise accuracy based on your real results.'],
];

export default function Landing() {
  return (
    <>
      <section className="cf-hero">
        <h1>Prepare for your first tech career with real practice</h1>
        <p className="cf-muted">
          CareerFit AI helps freshers choose a career path, practise with timed mock tests and see exactly which skills need work.
        </p>
        <Link className="cf-btn cf-btn-primary" to="/register">Create a free account</Link>
      </section>
      <section className="cf-content">
        <div className="cf-grid">
          {features.map(([title, text]) => (
            <div className="cf-card" key={title}><h3>{title}</h3><p className="cf-muted">{text}</p></div>
          ))}
        </div>
      </section>
    </>
  );
}
