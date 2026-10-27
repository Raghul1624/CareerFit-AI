import axios from 'axios';

const TOKEN_KEY = 'careerfit_token';

// Token storage: localStorage (simple, but readable by any script running on the page, so XSS
// would expose it). A production hardening step is an HttpOnly refresh-token cookie.
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url || '';
    if (err.response?.status === 401 && tokenStore.get() && !url.includes('/auth/login')) {
      tokenStore.clear();
      window.dispatchEvent(new Event('auth:expired'));
    }
    return Promise.reject(err);
  }
);

export function errorMessage(err) {
  const data = err?.response?.data;
  if (data?.fieldErrors) return Object.values(data.fieldErrors).join(' ');
  if (data?.message) return data.message;
  if (err?.request && !err?.response) return 'Cannot reach the server. Is the backend running?';
  return 'Something went wrong. Please try again.';
}

export default api;
