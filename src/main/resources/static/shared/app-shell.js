/* Khung giao diện dùng chung cho To-do list, Quản lí chi tiêu và trang giới thiệu:
   - Bỏ thanh nav phía trên (CSS ẩn .topbar).
   - Chuông thông báo + profile/đăng xuất chuyển xuống đáy sidebar.
   - Bộ chọn ứng dụng nằm ở cạnh phải và gồm To-do, Chi tiêu, Trang giới thiệu.
   Chỉ di chuyển các node có sẵn nên mọi sự kiện/ID của từng trang vẫn hoạt động. */
(() => {
    const topicBackgrounds = {
        tet: '/home/topics/tet.png',
        noel: '/home/topics/noel.png',
        ocean: '/home/topics/ocean.png'
    };
    const selectedTopic = localStorage.getItem('dayflow-topic-background');
    const selectedBackground = Object.prototype.hasOwnProperty.call(topicBackgrounds, selectedTopic)
        ? topicBackgrounds[selectedTopic]
        : '';
    const root = document.documentElement;
    if (selectedBackground) {
        root.dataset.topicBackground = selectedTopic;
        root.style.setProperty('--topic-background-image', `url("${selectedBackground}")`);
    } else if (selectedTopic) {
        localStorage.removeItem('dayflow-topic-background');
    }

    fetch('/api/packages/me', {credentials:'same-origin'})
        .then(response => response.ok ? response.json() : null)
        .then(plan => {
            if (!plan?.name) return;
            if (Number(plan.rank) < 2 && localStorage.getItem('dayflow-topic-background')) {
                localStorage.removeItem('dayflow-topic-background');
                delete root.dataset.topicBackground;
                root.style.removeProperty('--topic-background-image');
            }
            document.querySelectorAll('.workspace small, .workspace-sub').forEach(node => {
                if (node.closest('.workspace')) node.textContent = `Cá nhân · Gói ${plan.name}`;
            });
        })
        .catch(() => {});

    const topbar = document.querySelector('.topbar');
    const sidebar = document.querySelector('.sidebar');
    if (!topbar || !sidebar || document.querySelector('.side-account')) return;

    /* ---------- 1. Chuông + profile xuống đáy sidebar ---------- */
    const account = document.createElement('div');
    account.className = 'side-account';
    topbar.querySelectorAll('.icon-button, #user-menu, .logout-button, #logout-button, #logout, .profile, .avatar')
        .forEach(node => account.appendChild(node));      // giữ nguyên thứ tự gốc
    if (account.children.length) sidebar.appendChild(account);

    /* Admins can jump back to the admin workspace from any shared app page. */
    fetch('/api/auth/me', {credentials:'same-origin'})
        .then(response => response.ok ? response.json() : null)
        .then(user => {
            if (user?.role !== 'ADMIN') return;
            const adminLink = document.createElement('a');
            adminLink.href = '/admin/index.html';
            adminLink.className = 'side-link admin-return-link';
            adminLink.innerHTML = '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M9 14 4 19m0 0v-5m0 5h5M4 19a8 8 0 1 0 2-11" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M12 8v4l2.5 1.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg><span>Quay lại quản trị</span>';
            const sideBottom = sidebar.querySelector('.side-bottom');
            sidebar.insertBefore(adminLink, sideBottom || account);
        })
        .catch(() => {});

    /* ---------- 2. Ngăn kéo chọn ứng dụng ở cạnh phải ---------- */
    const switcher = topbar.querySelector('.app-switcher, .switcher');
    if (!switcher) return;

    const icons = {
        todo: '<svg viewBox="0 0 24 24" fill="none"><rect x="4" y="4" width="16" height="16" rx="4.5" stroke="currentColor" stroke-width="1.8"/><path d="m8.5 12.3 2.4 2.4 4.6-5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        expenses: '<svg viewBox="0 0 24 24" fill="none"><rect x="3.5" y="6" width="17" height="13" rx="3" stroke="currentColor" stroke-width="1.8"/><path d="M3.5 10h17M16 14.5h1.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M6.5 6 15 3.8l.9 2.2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        home: '<svg viewBox="0 0 24 24" fill="none"><path d="m3.5 10.5 8.5-7 8.5 7v9a1.5 1.5 0 0 1-1.5 1.5h-5v-6h-4v6h-5A1.5 1.5 0 0 1 3.5 19.5v-9Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>'
    };

    const dock = document.createElement('div');
    dock.className = 'app-dock';
    dock.innerHTML = `
        <button class="dock-toggle" type="button" aria-expanded="false" aria-controls="dock-panel" aria-label="Chọn ứng dụng" title="Chọn ứng dụng">
            <svg viewBox="0 0 24 24" fill="none"><path d="m14.5 6-6 6 6 6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
        <nav class="dock-panel" id="dock-panel" aria-label="Chọn ứng dụng">
            <div class="dock-title">Ứng dụng</div>
            <div class="dock-links"></div>
        </nav>`;
    const links = dock.querySelector('.dock-links');
    const homeLink = document.createElement('a');
    homeLink.href = '/';
    homeLink.className = 'dock-link';
    homeLink.textContent = 'Trang giới thiệu';
    const previousHomeLink = Array.from(sidebar.querySelectorAll('.side-nav a[href="/"]')).find(link => !link.closest('.brand'));
    const homeNav = previousHomeLink?.closest('nav');
    if (homeNav && homeNav.querySelectorAll('a').length === 1) homeNav.remove();

    [...switcher.querySelectorAll('a'), homeLink].forEach(link => {
        const href = link.getAttribute('href');
        const kind = href === '/' ? 'home' : href.includes('/expenses/') ? 'expenses' : 'todo';
        link.classList.add('dock-link');
        link.insertAdjacentHTML('afterbegin', `<span class="dock-ico dock-ico-${kind}">${icons[kind]}</span>`);
        links.appendChild(link);                           // giữ class active/aria-current gốc
    });
    document.body.appendChild(dock);

    const toggle = dock.querySelector('.dock-toggle');
    const setOpen = open => {
        dock.classList.toggle('open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Đóng danh sách ứng dụng' : 'Chọn ứng dụng');
    };
    toggle.addEventListener('click', event => { event.stopPropagation(); setOpen(!dock.classList.contains('open')); });
    document.addEventListener('click', event => { if (!dock.contains(event.target)) setOpen(false); });
    document.addEventListener('keydown', event => { if (event.key === 'Escape') setOpen(false); });
})();
