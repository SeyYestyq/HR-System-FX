'use strict';

const App = (() => {
  const STATUS_LABEL = { DRAFT: 'Черновик', OPEN: 'Открыта', PAUSED: 'Пауза', CLOSED: 'Закрыта' };
  const PRIORITY_LABEL = { HIGH: 'Высокий', MEDIUM: 'Средний', LOW: 'Низкий' };
  const ROLE_LABEL = { ADMIN: 'Админ', EDITOR: 'Редактор', VIEWER: 'Просмотр' };

  const $ = (id) => document.getElementById(id);

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>'"]/g, ch => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[ch]));
  }

  function money(value) {
    if (value == null || value === '') return '—';
    return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 }).format(Number(value));
  }

  function moneyRange(min, max) {
    if (min == null && max == null) return '—';
    return `${money(min)} – ${money(max)}`;
  }

  function when(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return new Intl.DateTimeFormat('ru-RU', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(date);
  }

  function showToast(message, kind) {
    let el = document.getElementById('appToast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'appToast';
      el.className = 'toast';
      document.body.appendChild(el);
    }
    el.textContent = message;
    el.className = 'toast' + (kind ? ' toast-' + kind : '');
    el.classList.add('is-visible');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => el.classList.remove('is-visible'), 4200);
  }

  function handleApiError(err, targetEl) {
    const msg = err && err.message ? err.message : 'Ошибка запроса';
    if (targetEl) targetEl.textContent = msg;
    if (err && err.conflict) showToast(msg, 'warn');
    return msg;
  }

  function applyRoleUi() {
    const canEdit = Api.canEdit();
    const admin = Api.isAdmin();
    document.querySelectorAll('.editor-only').forEach(el => el.classList.toggle('hidden', !canEdit));
    document.querySelectorAll('.admin-only').forEach(el => el.classList.toggle('hidden', !admin));
    const label = document.getElementById('userLabel');
    const user = Api.currentUser();
    if (label && user) {
      label.textContent = `${user.fullName} · ${ROLE_LABEL[user.role] || user.role}`;
    }
  }

  function renderNav(active) {
    const header = document.getElementById('topbar');
    if (!header) return;
    header.innerHTML = `
      <a class="brand" href="/vacancies.html"><span class="mark" aria-hidden="true"></span> Найм</a>
      <a class="link${active === 'vacancies' ? ' is-active' : ''}" href="/vacancies.html">Вакансии</a>
      <a class="link${active === 'analytics' ? ' is-active' : ''}" href="/analytics.html">Аналитика</a>
      <a class="link admin-only hidden${active === 'users' ? ' is-active' : ''}" href="/admin-users.html">Пользователи</a>
      <a class="link admin-only hidden${active === 'audit' ? ' is-active' : ''}" href="/admin-audit.html">Аудит</a>
      <span id="userLabel"></span>
      <button type="button" class="secondary" id="logoutBtn">Выход</button>
    `;
    document.getElementById('logoutBtn').addEventListener('click', async () => {
      await Api.logout();
      window.location.href = '/index.html';
    });
    applyRoleUi();
  }

  function bindLiveValidation(input, validateFn) {
    const wrap = input.closest('.field') || input.parentElement;
    let hint = wrap.querySelector('.field-hint');
    if (!hint) {
      hint = document.createElement('div');
      hint.className = 'field-hint';
      wrap.appendChild(hint);
    }
    const run = () => {
      const result = validateFn(input.value);
      const ok = result === true;
      input.classList.toggle('is-invalid', !ok);
      input.classList.toggle('is-valid', ok && String(input.value).trim() !== '');
      hint.textContent = ok ? '' : result;
    };
    input.addEventListener('input', run);
    input.addEventListener('blur', run);
    return run;
  }

  function validateTitle(value) {
    const v = String(value || '').trim();
    if (v.length < 3) return 'Минимум 3 символа';
    if (v.length > 150) return 'Максимум 150 символов';
    return true;
  }

  function validateSalary(minEl, maxEl) {
    const min = minEl.value === '' ? null : Number(minEl.value);
    const max = maxEl.value === '' ? null : Number(maxEl.value);
    if (min != null && min < 0) return 'Зарплата не может быть отрицательной';
    if (max != null && max < 0) return 'Зарплата не может быть отрицательной';
    if (min != null && max != null && max < min) return 'Максимум должен быть не меньше минимума';
    return true;
  }

  function createTagEditor(container, initial) {
    const state = new Set([...(initial || [])].map(t => String(t).trim().toLowerCase()).filter(Boolean));
    const render = () => {
      container.innerHTML = `
        <div class="tag-chips">${[...state].map(t => `
          <span class="tag-chip">${escapeHtml(t)}
            <button type="button" data-tag="${escapeHtml(t)}" aria-label="Удалить тег">×</button>
          </span>`).join('')}</div>
        <div class="tag-input-row">
          <input type="text" class="tag-input" placeholder="тег и Enter"/>
        </div>`;
      container.querySelectorAll('[data-tag]').forEach(btn => {
        btn.addEventListener('click', () => {
          state.delete(btn.getAttribute('data-tag'));
          render();
        });
      });
      const input = container.querySelector('.tag-input');
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ',') {
          e.preventDefault();
          const raw = input.value.replace(/,/g, '').trim().toLowerCase();
          if (raw) state.add(raw);
          input.value = '';
          render();
          container.querySelector('.tag-input').focus();
        }
      });
    };
    render();
    return {
      values: () => [...state],
      set: (tags) => {
        state.clear();
        (tags || []).forEach(t => {
          const v = String(t).trim().toLowerCase();
          if (v) state.add(v);
        });
        render();
      }
    };
  }

  return {
    $, escapeHtml, money, moneyRange, when, showToast, handleApiError,
    applyRoleUi, renderNav, bindLiveValidation, validateTitle, validateSalary,
    createTagEditor, STATUS_LABEL, PRIORITY_LABEL, ROLE_LABEL
  };
})();
