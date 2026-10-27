/**
 * Filters for the test list: type (all / career / skill), career path, skill and free-text search.
 * Fully controlled; the page keeps the values in the URL so they survive navigation.
 */
export default function TestFilters({ values, careerPaths, skills, onChange }) {
  const set = (patch) => onChange({ ...values, ...patch });
  const types = [
    { value: '', label: 'All Tests' },
    { value: 'CAREER', label: 'Career Mock Tests' },
    { value: 'SKILL', label: 'Skill Tests' },
  ];

  return (
    <div className="cf-card cf-filters">
      <div className="cf-tabs" role="tablist" aria-label="Test type">
        {types.map((t) => (
          <button key={t.value || 'all'} type="button" role="tab" aria-selected={values.mode === t.value}
            className={values.mode === t.value ? 'active' : ''} onClick={() => set({ mode: t.value })}>
            {t.label}
          </button>
        ))}
      </div>
      <div className="cf-filter-row">
        <select className="cf-input" aria-label="Filter by career path" value={values.careerPathId}
          onChange={(e) => set({ careerPathId: e.target.value, mode: e.target.value ? 'CAREER' : values.mode })}>
          <option value="">All career paths</option>
          {careerPaths.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
        <select className="cf-input" aria-label="Filter by skill" value={values.skillId}
          onChange={(e) => set({ skillId: e.target.value })}>
          <option value="">All skills</option>
          {skills.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <input className="cf-input" type="search" placeholder="Search topics, e.g. JDBC or joins" aria-label="Search tests"
          value={values.q} onChange={(e) => set({ q: e.target.value })} />
      </div>
    </div>
  );
}
