import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api, { tokenStore } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!tokenStore.get());

  useEffect(() => {
    if (!tokenStore.get()) return;
    api.get('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener('auth:expired', onExpired);
    return () => window.removeEventListener('auth:expired', onExpired);
  }, []);

  const handleAuth = useCallback((data) => {
    tokenStore.set(data.token);
    setUser(data.user);
  }, []);

  const login = useCallback(async (email, password) => {
    handleAuth((await api.post('/auth/login', { email, password })).data);
  }, [handleAuth]);

  const register = useCallback(async (fullName, email, password) => {
    handleAuth((await api.post('/auth/register', { fullName, email, password })).data);
  }, [handleAuth]);

  const logout = useCallback(async () => {
    try { await api.post('/auth/logout'); } catch { /* token is discarded client-side regardless */ }
    tokenStore.clear();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout, isAdmin: !!user?.roles?.includes('ADMIN') }),
    [user, loading, login, register, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
