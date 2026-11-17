
'use strict';

const state = {
  accessToken: null,
  user: null,
  page: 0,
  size: 20,
  totalPages: 0,
  currentVacancy: null,
  employers: [],
  selected: new Set(),
  refreshPromise: null
};

const $ = (id) => document.getElementById(id);
const roles = () => new Set([state.user?.role].filter(Boolean));
const isAdmin = () => roles().has('ADMIN');
const canEdit = () => roles().has('EDITOR') || isAdmin();

async function api(method, url, body, isForm = false) {
  const headers = {};
  if (state.accessToken) headers.Authorization = `Bearer ${state.accessToken}`;
  if (body && !isForm) headers['Content-Type'] = 'application/json';
  let response = await fetch(url, {
    method,
    headers,
    credentials: 'include',
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined
  });
  if (response.status === 401 && !url.includes('/api/auth/login')) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      headers.Authorization = `Bearer ${state.accessToken}`;
      response = await fetch(url, {
        method, headers, credentials: 'include',
        body: body ? (isForm ? body : JSON.stringify(body)) : undefined
      });
    }
  }
  if (response.status === 204) return null;
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message = data?.message || `Ошибка ${response.status}`;
    const err = new Error(message);
    err.status = response.status;
    err.data = data;
    throw err;
  }
  return data;
}

async function tryRefresh() {
  if (state.refreshPromise) return state.refreshPromise;
  state.refreshPromise = (async () => {
    try {
      const data = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
        .then(r => r.ok ? r.json() : null);
      if (!data) return false;
      state.accessToken = data.accessToken;
      state.user = data.user;
      return true;
    } catch {
      return false;
    } finally {
      state.refreshPromise = null;
    }
  })();
  return state.refreshPromise;
}

const STATUS_LABEL = { DRAFT: 'Черновик', OPEN: 'Открыта', PAUSED: 'Пауза', CLOSED: 'Закрыта' };
const PRIORITY_LABEL = { HIGH: 'Высокий', MEDIUM: 'Средний', LOW: 'Низкий' };
const ROLE_LABEL = { ADMIN: 'Админ', EDITOR: 'Редактор', VIEWER: 'Просмотр' };

function money(value) {
  if (value == null || value === '') return '—';
  return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 }).format(Number(value));
}

function moneyRange(min, max) {
  if (min == null && max == null) return '—';
  return `${money(min)} – ${money(max)}`;
}

function when(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  }).format(date);
}

function show(view) {
  ['login','password','vacancies','analytics','users','audit'].forEach(v => {
    $(`view-${v}`).classList.toggle('hidden', v !== view);
  });
  $('topbar').classList.toggle('hidden', view === 'login' || view === 'password');
  document.querySelectorAll('[data-nav]').forEach(btn => {
    btn.classList.toggle('is-active', btn.getAttribute('data-nav') === view);
  });
}

function applyRoleUi() {
  document.querySelectorAll('.admin-only').forEach(el => el.classList.toggle('hidden', !isAdmin()));
  document.querySelectorAll('.editor-only').forEach(el => el.classList.toggle('hidden', !canEdit()));
  $('userLabel').textContent = state.user
    ? `${state.user.fullName} · ${ROLE_LABEL[state.user.role] || state.user.role}`
    : '';
}

async function afterLogin(data) {
  state.accessToken = data.accessToken;
  state.user = data.user;
  applyRoleUi();
  if (data.mustChangePassword || data.user?.mustChangePassword) {
    show('password');
    return;
  }
  await loadEmployers();
  show('vacancies');
  await loadVacancies();
}

$('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('loginError').textContent = '';
  try {
    const data = await api('POST', '/api/auth/login', {
      email: $('loginEmail').value,
      password: $('loginPassword').value
    });
    await afterLogin(data);
  } catch (err) {
    $('loginError').textContent = err.message;
  }
});

$('passwordForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('passwordError').textContent = '';
  if ($('newPassword').value !== $('confirmPassword').value) {
    $('passwordError').textContent = 'Пароли не совпадают';
    return;
  }
  try {
    await api('POST', '/api/auth/change-password', {
      currentPassword: $('currentPassword').value,
      newPassword: $('newPassword').value
    });
    state.user.mustChangePassword = false;
    await loadEmployers();
    show('vacancies');
    await loadVacancies();
  } catch (err) {
    $('passwordError').textContent = err.message;
  }
});

$('logoutBtn').addEventListener('click', async () => {
  try { await api('POST', '/api/auth/logout'); } catch {}
  state.accessToken = null;
  state.user = null;
  show('login');
});

document.querySelectorAll('[data-nav]').forEach(btn => {
  btn.addEventListener('click', async () => {
    const view = btn.getAttribute('data-nav');
    show(view);
    if (view === 'vacancies') await loadVacancies();
    if (view === 'analytics') await loadAnalytics();
    if (view === 'users') await loadUsers();
    if (view === 'audit') await loadAudit();
  });
});

function filterParams() {
  const p = new URLSearchParams();
  const map = {
    q: 'fQ', employerUuid: 'fEmployer', status: 'fStatus', remote: 'fRemote',
    priority: 'fPriority', tag: 'fTag', archived: 'fArchived',
    salaryFrom: 'fSalaryFrom', salaryTo: 'fSalaryTo'
  };
  Object.entries(map).forEach(([k, id]) => {
    const v = $(id).value;
    if (v !== '') p.set(k, v);
  });
  p.set('page', String(state.page));
  p.set('size', String(state.size));
  p.set('sort', 'createdAt,desc');
  return p;
}

async function loadEmployers() {
  state.employers = await api('GET', '/api/employers?onlyEnabled=false');
  const options = state.employers.map(e => `<option value="${e.uuid}">${e.name}</option>`).join('');
  $('fEmployer').innerHTML = '<option value="">Все</option>' + options;
  $('vEmployer').innerHTML = state.employers.filter(e => e.enabled).map(e => `<option value="${e.uuid}">${e.name}</option>`).join('');
}

async function loadVacancies() {
  const data = await api('GET', `/api/vacancies?${filterParams()}`);
  state.totalPages = data.totalPages;
  $('vacancyMeta').textContent = `${data.content.length} из ${data.totalElements}`;
  $('pageLabel').textContent = `${data.page + 1} / ${Math.max(data.totalPages, 1)}`;
  $('vacancyRows').innerHTML = data.content.map(v => `
    <tr>
      <td><input type="checkbox" data-id="${v.uuid}" ${state.selected.has(v.uuid) ? 'checked' : ''}/></td>
      <td><a class="code" href="#" data-open="${v.uuid}">${escapeHtml(v.code)}</a></td>
      <td>${escapeHtml(v.title)}</td>
      <td>${escapeHtml(v.employerName)}</td>
      <td>${STATUS_LABEL[v.status] || escapeHtml(v.status)}</td>
      <td class="num mono">${moneyRange(v.salaryMin, v.salaryMax)}</td>
      <td><span class="stamp ${String(v.priority).toLowerCase()}">${PRIORITY_LABEL[v.priority] || escapeHtml(v.priority)}</span></td>
      <td>${[...v.tags || []].map(t => `<span class="tag">${escapeHtml(t)}</span>`).join('')}</td>
    </tr>`).join('');
  $('vacancyRows').querySelectorAll('[data-open]').forEach(a => a.addEventListener('click', async (e) => {
    e.preventDefault();
    await openVacancy(a.getAttribute('data-open'));
  }));
  $('vacancyRows').querySelectorAll('input[type=checkbox]').forEach(cb => cb.addEventListener('change', () => {
    if (cb.checked) state.selected.add(cb.dataset.id); else state.selected.delete(cb.dataset.id);
  }));
}

async function openVacancy(uuid) {
  const v = await api('GET', `/api/vacancies/${uuid}`);
  state.currentVacancy = v;
  $('editCard').classList.remove('hidden');
  $('editTitle').textContent = `${v.code} · ${v.title}`;
  $('vTitle').value = v.title;
  $('vEmployer').value = v.employerUuid;
  $('vStatus').value = v.status;
  $('vSalaryMin').value = v.salaryMin ?? '';
  $('vSalaryMax').value = v.salaryMax ?? '';
  $('vRemote').value = String(v.remote);
  $('vDescription').value = v.description || '';
  $('vTags').value = [...(v.tags || [])].join(', ');
  const notes = await api('GET', `/api/vacancies/${uuid}/notes`);
  $('notesList').innerHTML = notes.map(n => `
    <div class="note">
      <div class="note-head">
        <strong>${escapeHtml(n.authorName)}</strong>
        ${n.pinned ? '<span class="pin">Закреплена</span>' : ''}
        <span class="muted">${when(n.createdAt)}</span>
      </div>
      <div>${escapeHtml(n.content)}</div>
    </div>`).join('') || '<div class="muted">Заметок нет</div>';
}

$('searchBtn').addEventListener('click', async () => { state.page = 0; await loadVacancies(); });
$('prevPage').addEventListener('click', async () => { if (state.page > 0) { state.page--; await loadVacancies(); } });
$('nextPage').addEventListener('click', async () => { if (state.page + 1 < state.totalPages) { state.page++; await loadVacancies(); } });

$('createBtn').addEventListener('click', () => {
  state.currentVacancy = null;
  $('editCard').classList.remove('hidden');
  $('editTitle').textContent = 'Новая вакансия';
  $('vacancyForm').reset();
  $('notesList').innerHTML = '';
});

$('saveVacancyBtn').addEventListener('click', async () => {
  $('vacancyError').textContent = '';
  const body = {
    title: $('vTitle').value,
    description: $('vDescription').value,
    employerUuid: $('vEmployer').value,
    status: $('vStatus').value,
    salaryMin: $('vSalaryMin').value ? Number($('vSalaryMin').value) : null,
    salaryMax: $('vSalaryMax').value ? Number($('vSalaryMax').value) : null,
    remote: $('vRemote').value === 'true',
    tags: $('vTags').value.split(',').map(s => s.trim()).filter(Boolean),
    version: state.currentVacancy?.version ?? null
  };
  try {
    if (state.currentVacancy) {
      await api('PUT', `/api/vacancies/${state.currentVacancy.uuid}`, body);
    } else {
      await api('POST', '/api/vacancies', body);
    }
    await loadVacancies();
  } catch (err) {
    $('vacancyError').textContent = err.message;
  }
});

$('archiveVacancyBtn').addEventListener('click', async () => {
  if (!state.currentVacancy) return;
  await api('DELETE', `/api/vacancies/${state.currentVacancy.uuid}`);
  await loadVacancies();
  await openVacancy(state.currentVacancy.uuid);
});

$('restoreVacancyBtn').addEventListener('click', async () => {
  if (!state.currentVacancy) return;
  await api('POST', `/api/vacancies/${state.currentVacancy.uuid}/restore`);
  await loadVacancies();
  await openVacancy(state.currentVacancy.uuid);
});

$('deleteVacancyBtn').addEventListener('click', async () => {
  if (!state.currentVacancy) return;
  await api('DELETE', `/api/vacancies/${state.currentVacancy.uuid}/permanent`, null);
  // permanent needs header - use fetch directly
});

$('deleteVacancyBtn').onclick = async () => {
  if (!state.currentVacancy) return;
  const response = await fetch(`/api/vacancies/${state.currentVacancy.uuid}/permanent`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${state.accessToken}`,
      'X-Confirm-Delete': 'PERMANENTLY_DELETE'
    },
    credentials: 'include'
  });
  if (!response.ok) {
    const data = await response.json();
    $('vacancyError').textContent = data.message || 'Ошибка удаления';
    return;
  }
  state.currentVacancy = null;
  $('editCard').classList.add('hidden');
  await loadVacancies();
};

$('bulkArchiveBtn').addEventListener('click', async () => {
  if (!state.selected.size) return;
  await api('POST', '/api/vacancies/bulk', {
    action: 'ARCHIVE',
    uuids: [...state.selected]
  });
  state.selected.clear();
  await loadVacancies();
});

$('exportBtn').addEventListener('click', async () => {
  const response = await fetch(`/api/vacancies/export?${filterParams()}`, {
    headers: { Authorization: `Bearer ${state.accessToken}` },
    credentials: 'include'
  });
  const blob = await response.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'vacancies.csv';
  a.click();
});

$('importFile').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const form = new FormData();
  form.append('file', file);
  const preview = await api('POST', '/api/vacancies/import/preview?mode=UPSERT_BY_CODE', form, true);
  if (confirm(`Корректных: ${preview.validRows}, ошибок: ${preview.invalidRows}. Применить?`)) {
    await api('POST', '/api/vacancies/import/apply?mode=UPSERT_BY_CODE', form, true);
    await loadVacancies();
  }
  e.target.value = '';
});

$('noteForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!state.currentVacancy) return;
  await api('POST', `/api/vacancies/${state.currentVacancy.uuid}/notes`, {
    content: $('noteContent').value,
    pinned: $('notePinned').checked
  });
  $('noteContent').value = '';
  await openVacancy(state.currentVacancy.uuid);
});

async function loadAnalytics() {
  const data = await api('GET', '/api/analytics/summary');
  const figures = [
    ['Всего', data.total, false],
    ['Активные', data.active, false],
    ['Архив', data.archived, false],
    ['Удалённо', data.remoteCount, false],
    ['Средняя вилка', money(data.averageSalaryMin), true]
  ];
  $('analyticsCards').innerHTML = figures.map(([label, value, accent]) => `
    <div class="stat">
      <div class="label">${label}</div>
      <div class="metric${accent ? ' accent' : ''}">${value}</div>
    </div>`).join('');
  const employers = data.byEmployer || [];
  const maxEmployer = Math.max(1, ...employers.map(r => r.count));
  const lead = employers.reduce((best, row) => row.count > (best?.count || 0) ? row : best, null);
  $('byEmployer').innerHTML = employers.map(r => `
    <div class="bar-row">
      <div>
        <div>${escapeHtml(r.name)}</div>
        <div class="bar-track"><div class="bar-fill${lead && r.name === lead.name ? ' lead' : ''}" style="width:${Math.round(r.count / maxEmployer * 100)}%"></div></div>
      </div>
      <span class="count">${r.count}</span>
    </div>`).join('') || '<div class="muted">Нет данных</div>';
  const quiet = (rows, labels) => (rows || []).map(r => `
    <div class="quiet-row"><span>${escapeHtml(labels[r.name] || r.name)}</span><span class="count">${r.count}</span></div>`).join('') || '<div class="muted">Нет данных</div>';
  $('byStatus').innerHTML = quiet(data.byStatus, STATUS_LABEL);
  $('byPriority').innerHTML = quiet(data.byPriority, PRIORITY_LABEL);
}

async function loadUsers() {
  const data = await api('GET', '/api/admin/users?page=0&size=50');
  $('userRows').innerHTML = data.content.map(u => `
    <tr>
      <td>${escapeHtml(u.email)}</td>
      <td>${escapeHtml(u.fullName)}</td>
      <td>${ROLE_LABEL[u.role] || escapeHtml(u.role)}</td>
      <td>${u.enabled ? 'Да' : 'Нет'}</td>
      <td><button data-reset="${u.uuid}" class="secondary">Сброс пароля</button></td>
    </tr>`).join('');
  $('userRows').querySelectorAll('[data-reset]').forEach(btn => btn.addEventListener('click', async () => {
    const res = await api('POST', `/api/admin/users/${btn.dataset.reset}/reset-password`);
    $('tempPassword').textContent = `Временный пароль: ${res.temporaryPassword}`;
  }));
}

$('createUserForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const res = await api('POST', '/api/admin/users', {
    email: $('uEmail').value,
    fullName: $('uName').value,
    role: $('uRole').value
  });
  $('tempPassword').textContent = `Создан. Временный пароль: ${res.temporaryPassword}`;
  await loadUsers();
});

async function loadAudit() {
  const q = $('auditQ').value;
  const data = await api('GET', `/api/admin/audit?q=${encodeURIComponent(q)}&page=0&size=50`);
  $('auditRows').innerHTML = data.content.map(a => `
    <tr>
      <td class="mono">${when(a.createdAt)}</td>
      <td>${escapeHtml(a.actorEmail || '')}</td>
      <td>${escapeHtml(a.eventType)}</td>
      <td>${escapeHtml(a.description)}</td>
    </tr>`).join('');
}
$('auditSearchBtn').addEventListener('click', loadAudit);

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, ch => ({
    '&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'
  }[ch]));
}

show('login');
