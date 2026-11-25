'use strict';

const Api = (() => {
  const TOKEN_KEY = 'hr_access_token';
  let refreshPromise = null;
  let cachedUser = null;

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setToken(token) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  }

  function clearSession() {
    setToken(null);
    cachedUser = null;
  }

  function currentUser() {
    return cachedUser;
  }

  function hasRole(...roles) {
    return cachedUser != null && roles.includes(cachedUser.role);
  }

  function canEdit() {
    return hasRole('EDITOR', 'ADMIN');
  }

  function isAdmin() {
    return hasRole('ADMIN');
  }

  async function request(method, url, body, options = {}) {
    const headers = Object.assign({}, options.headers || {});
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
    const isForm = body instanceof FormData;
    if (body != null && !isForm && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    let response = await fetch(url, {
      method,
      headers,
      credentials: 'include',
      body: body == null ? undefined : (isForm ? body : JSON.stringify(body))
    });

    if (response.status === 401 && !String(url).includes('/api/auth/login') && !options._retried) {
      const ok = await tryRefresh();
      if (ok) {
        return request(method, url, body, Object.assign({}, options, { _retried: true }));
      }
      clearSession();
      if (!options.skipRedirect) {
        window.location.href = '/index.html';
      }
      const err = new Error('Требуется вход');
      err.status = 401;
      throw err;
    }

    if (response.status === 204) return null;

    const text = await response.text();
    let data = null;
    if (text) {
      try { data = JSON.parse(text); } catch { data = { message: text }; }
    }

    if (!response.ok) {
      const message = (data && (data.message || data.error)) || `Ошибка ${response.status}`;
      const err = new Error(message);
      err.status = response.status;
      err.data = data;
      if (response.status === 409) {
        err.conflict = true;
        err.message = 'Запись изменил другой пользователь. Обновите данные и повторите.';
      }
      throw err;
    }
    return data;
  }

  async function tryRefresh() {
    if (refreshPromise) return refreshPromise;
    refreshPromise = (async () => {
      try {
        const response = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include'
        });
        if (!response.ok) return false;
        const data = await response.json();
        setToken(data.accessToken);
        cachedUser = data.user || cachedUser;
        return true;
      } catch {
        return false;
      } finally {
        refreshPromise = null;
      }
    })();
    return refreshPromise;
  }

  async function login(email, password) {
    const data = await request('POST', '/api/auth/login', { email, password }, { skipRedirect: true });
    setToken(data.accessToken);
    cachedUser = data.user;
    return data;
  }

  async function logout() {
    try { await request('POST', '/api/auth/logout', null, { skipRedirect: true }); } catch (_) {}
    clearSession();
  }

  async function me() {
    cachedUser = await request('GET', '/api/auth/me');
    return cachedUser;
  }

  async function changePassword(currentPassword, newPassword) {
    return request('POST', '/api/auth/change-password', { currentPassword, newPassword });
  }

  async function ensureAuth(options = {}) {
    if (!getToken()) {
      window.location.href = '/index.html';
      return null;
    }
    try {
      const user = await me();
      if (options.requireAdmin && user.role !== 'ADMIN') {
        window.location.href = '/vacancies.html';
        return null;
      }
      if (user.mustChangePassword && !options.allowMustChange) {
        window.location.href = '/index.html#change-password';
        return null;
      }
      return user;
    } catch (err) {
      clearSession();
      window.location.href = '/index.html';
      return null;
    }
  }

  return {
    request, login, logout, me, changePassword, ensureAuth,
    getToken, setToken, clearSession, currentUser, hasRole, canEdit, isAdmin
  };
})();
