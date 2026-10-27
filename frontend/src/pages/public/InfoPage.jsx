const content = {
  about: {
    title: 'About CareerFit AI',
    text: 'CareerFit AI is a career preparation platform for freshers. Resume analysis, job matching and learning roadmaps are planned for later phases; career paths, mock tests, results and history are available now.',
  },
  features: {
    title: 'Features',
    text: 'Available now: registration and login, career paths with required skills, timed mock tests with server-side scoring, result review with explanations, test history and a personal dashboard.',
  },
};

export default function InfoPage({ kind }) {
  const page = content[kind];
  return (
    <div className="cf-content">
      <div className="cf-card"><h1>{page.title}</h1><p className="cf-muted">{page.text}</p></div>
    </div>
  );
}
