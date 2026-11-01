/* Dropdown avatar dùng chung cho thanh nav phía trên.
   Dùng: <div class="user-menu" id="user-menu"></div> trong topbar + nạp file này (defer). */
(() => {
    const host = document.getElementById('user-menu');
    if (!host) return;
    const ico = {
        profile:'<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="3.6" stroke="currentColor" stroke-width="1.7"/><path d="M4.5 20c.8-3.6 3.8-5.5 7.5-5.5s6.7 1.9 7.5 5.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        settings:'<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.7"/><path d="M19.4 14.5a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3h0a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5h0a1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8v0a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>',
        themes:'<svg viewBox="0 0 24 24" fill="none"><path d="M12 3a9 9 0 1 0 0 18h1.2a2.2 2.2 0 0 0 1.7-3.6c-.8-1 .2-2.4 1.5-2.4H18a3 3 0 0 0 3-3C21 7 17 3 12 3Z" stroke="currentColor" stroke-width="1.7"/><circle cx="7.5" cy="10" r="1" fill="currentColor"/><circle cx="11" cy="7" r="1" fill="currentColor"/><circle cx="16" cy="9" r="1" fill="currentColor"/></svg>',
        packages:'<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="2.5" stroke="currentColor" stroke-width="1.7"/><path d="M3 9h18M7 14h4m7-1v4m-2-2h4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
        logout:'<svg viewBox="0 0 24 24" fill="none"><path d="M9 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h3M16 8l4 4-4 4M20 12H9" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
    };
    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const initials = name => (name || '').trim().split(/\s+/).slice(-2).map(p => p[0]).join('').toUpperCase() || 'U';
    const roleLabel = role => ({ADMIN:'Quản trị viên', USER:'Người dùng'}[role] || role || '—');
    const activeView = document.body.dataset.view || '';

    host.classList.add('user-menu');
    host.innerHTML = `
        <button class="user-menu-trigger" id="user-menu-trigger" type="button" aria-haspopup="menu" aria-expanded="false" aria-label="Tài khoản">…</button>
        <div class="user-menu-panel" id="user-menu-panel" role="menu" aria-label="Menu tài khoản">
            <div class="user-menu-head">
                <span class="user-menu-big" id="um-big">…</span>
                <span class="user-menu-name" id="um-name">Đang tải…</span>
                <span class="user-menu-email" id="um-email"></span>
            </div>
            <div class="user-menu-list">
                <button class="user-menu-item" type="button" role="menuitem" data-action="profile">${ico.profile}<span>Hồ sơ</span></button>
                <button class="user-menu-item" type="button" role="menuitem" data-action="settings">${ico.settings}<span>Cài đặt</span></button>
            </div>
            <div class="user-menu-list">
                <a class="user-menu-item${activeView === 'themes' ? ' active' : ''}" role="menuitem" data-nav="themes" href="/todo/themes/index.html">${ico.themes}<span>Chủ đề</span></a>
                <a class="user-menu-item${activeView === 'packages' ? ' active' : ''}" role="menuitem" data-nav="packages" href="/todo/packages/index.html">${ico.packages}<span>Gói nạp</span></a>
            </div>
            <div class="user-menu-list">
                <button class="user-menu-item danger" type="button" role="menuitem" data-action="logout">${ico.logout}<span>Đăng xuất</span></button>
            </div>
        </div>`;

    const trigger = host.querySelector('#user-menu-trigger');
    let user = null;
    const setOpen = open => { host.classList.toggle('open', open); trigger.setAttribute('aria-expanded', String(open)); };

    trigger.addEventListener('click', e => { e.stopPropagation(); setOpen(!host.classList.contains('open')); });
    document.addEventListener('click', e => { if (!host.contains(e.target)) setOpen(false); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') { setOpen(false); trigger.focus(); } });

    const dialog = document.createElement('dialog');
    dialog.className = 'um-dialog';
    document.body.appendChild(dialog);
    dialog.addEventListener('click', e => { if (e.target === dialog) dialog.close(); });
    const openDialog = (title, sub, rowsHtml) => {
        dialog.innerHTML = `<div class="um-dialog-inner"><h2>${esc(title)}</h2><p>${esc(sub)}</p><dl class="um-rows">${rowsHtml}</dl><div class="um-dialog-actions"><button class="um-close" type="button">Đóng</button></div></div>`;
        dialog.querySelector('.um-close').addEventListener('click', () => dialog.close());
        dialog.showModal();
    };
    const row = (k, v) => `<div class="um-row"><dt>${esc(k)}</dt><dd>${esc(v)}</dd></div>`;

    const logout = async () => {
        try {
            await fetch('/api/auth/csrf', {credentials:'same-origin'});
            const cookie = document.cookie.split('; ').find(v => v.startsWith('XSRF-TOKEN='));
            await fetch('/api/auth/logout', {method:'POST', credentials:'same-origin', headers:{'X-XSRF-TOKEN': cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : ''}});
        } finally { location.href = '/login/index.html'; }
    };

    host.addEventListener('click', e => {
        const item = e.target.closest('[data-action]');
        if (!item) return;
        setOpen(false);
        const action = item.dataset.action;
        if (action === 'logout') logout();
        else if (action === 'profile') openDialog('Hồ sơ', 'Thông tin tài khoản của bạn.',
            row('Họ và tên', user?.fullName) + row('Email', user?.email) + row('Vai trò', roleLabel(user?.role)));
        else if (action === 'settings') openDialog('Cài đặt', 'Tuỳ chọn tài khoản.',
            row('Giao diện', 'Đổi trong mục Chủ đề') + row('Gói dịch vụ', 'Xem trong mục Gói nạp'));
    });

    fetch('/api/auth/me', {credentials:'same-origin'}).then(r => r.ok ? r.json() : null).then(u => {
        if (!u) return;
        user = u;
        const ini = initials(u.fullName);
        trigger.textContent = ini;
        host.querySelector('#um-big').textContent = ini;
        host.querySelector('#um-name').textContent = u.fullName || '';
        host.querySelector('#um-email').textContent = u.email || '';
        trigger.title = u.email || '';
    }).catch(() => {});
})();
