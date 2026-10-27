import api from './client.js';

/** Thin wrappers around the mock-test endpoints; each returns the response body. */
export const listMockTests = (params = {}) => api.get('/mock-tests', { params }).then((r) => r.data);
export const getMockTest = (id) => api.get(`/mock-tests/${id}`).then((r) => r.data);
export const startAttempt = (testId) => api.post(`/mock-tests/${testId}/attempts`).then((r) => r.data);
export const listCareerPaths = () => api.get('/career-paths').then((r) => r.data);
export const listSkills = () => api.get('/skills').then((r) => r.data);
