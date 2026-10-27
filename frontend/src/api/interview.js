import api from './client.js';

/** Wrappers around the Interview Preparation endpoints. Each returns the response body. */
export const listRoles = () => api.get('/interview/roles').then((r) => r.data);
export const getRole = (code) => api.get(`/interview/roles/${code}`).then((r) => r.data);
export const getOverview = () => api.get('/interview/overview').then((r) => r.data);
export const startInterview = (roleCode, round, level) =>
  api.post('/interview/attempts', { roleCode, round, level }).then((r) => r.data);
export const getAttempt = (id) => api.get(`/interview/attempts/${id}`).then((r) => r.data);
export const saveAnswers = (id, answers) => api.put(`/interview/attempts/${id}/answers`, { answers });
export const submitAttempt = (id) => api.post(`/interview/attempts/${id}/submit`).then((r) => r.data);
export const getResult = (id) => api.get(`/interview/attempts/${id}/result`).then((r) => r.data);
export const getHistory = (params) => api.get('/interview/attempts/history', { params }).then((r) => r.data);

export const getCodingStatus = () => api.get('/interview/coding/status').then((r) => r.data);

// Running code in the sandbox can take several seconds (compile + every test case), so these calls
// get a much longer timeout than the 15 s default of the shared client.
const CODE_TIMEOUT_MS = 180000;
export const runCode = (attemptId, questionId, body) =>
  api.post(`/interview/attempts/${attemptId}/questions/${questionId}/run`, body, { timeout: CODE_TIMEOUT_MS }).then((r) => r.data);
export const submitCode = (attemptId, questionId, body) =>
  api.post(`/interview/attempts/${attemptId}/questions/${questionId}/submit-code`, body, { timeout: CODE_TIMEOUT_MS }).then((r) => r.data);
export const listSubmissions = (attemptId, questionId) =>
  api.get(`/interview/attempts/${attemptId}/questions/${questionId}/submissions`).then((r) => r.data);
