(() => {
    let USER_ID;
    const monthPicker = document.getElementById('month-picker');
    const list = document.getElementById('transaction-list');
    const search = document.getElementById('search');
    const dialog = document.getElementById('transaction-dialog');
    const form = document.getElementById('transaction-form');
    const typeSelect = document.getElementById('transaction-type');
    const categorySelect = document.getElementById('transaction-category');
    const walletSelect = document.getElementById('transaction-wallet');
    let transactions = [];
    let categories = [];
    let currentFilter = 'all';
    const money = new Intl.NumberFormat('vi-VN', { style:'currency', currency:'VND', maximumFractionDigits:0 });
    const escapeHtml = (text) => String(text ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    const selectedMonth = () => monthPicker.value;
    function setMessage(id, message, visible) {
        const element = document.getElementById(id);
        if (message !== undefined) element.textContent = message;
        element.classList.toggle('visible', visible);
    }
    async function api(path, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const csrfItem = () => document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='));
        const headers = { ...(options.body ? {'Content-Type':'application/json'} : {}), ...(options.headers || {}) };
        if (!['GET','HEAD','OPTIONS'].includes(method)) {
            if (!csrfItem()) await fetch('/api/auth/csrf', { credentials:'same-origin' });
            const tokenCookie = csrfItem();
            if (tokenCookie) headers['X-XSRF-TOKEN'] = decodeURIComponent(tokenCookie.slice('XSRF-TOKEN='.length));
        }
        const response = await fetch(path, {
            ...options,
            credentials:'same-origin',
            headers
        });
        if (response.status === 401) {
            location.href = `/login/index.html?next=${encodeURIComponent(location.pathname)}`;
            throw new Error('Vui lòng đăng nhập để tiếp tục.');
        }
        if (!response.ok) {
            const body = await response.json().catch(() => null);
            throw new Error(body?.detail || body?.message || `Yêu cầu thất bại (${response.status})`);
        }
        return response.status === 204 ? null : response.json();
    }
    function formatDate(value) {
        return new Intl.DateTimeFormat('vi-VN', { day:'numeric', month:'short', year:'numeric' }).format(new Date(value));
    }
    async function loadCategories() {
        categories = await api(`/api/finance/categories?userId=${USER_ID}&type=${typeSelect.value}`);
        categorySelect.innerHTML = '<option value="">Chưa phân loại</option>' + categories.map(category =>
            `<option value="${category.id}">${escapeHtml(category.name)}</option>`).join('');
    }
    async function loadWallets() {
        const wallets = await api(`/api/finance/wallets?userId=${USER_ID}`);
        walletSelect.innerHTML = '<option value="">Chọn ví</option>' + wallets.map(wallet =>
            `<option value="${wallet.id}">${escapeHtml(wallet.name)} · ${money.format(wallet.balance)}</option>`).join('');
        walletSelect.disabled = wallets.length === 0;
    }
    function renderTransactions() {
        const query = search.value.trim().toLocaleLowerCase('vi');
        const filtered = transactions.filter(transaction => {
            const text = `${transaction.note || ''} ${transaction.categoryName} ${transaction.walletName}`.toLocaleLowerCase('vi');
            return (currentFilter === 'all' || transaction.type === currentFilter) && text.includes(query);
        });
        list.innerHTML = filtered.map(transaction => {
            const income = transaction.type === 'income';
            const label = transaction.note || transaction.categoryName || (income ? 'Khoản thu' : 'Khoản chi');
            const icon = income
                ? '<path d="M12 19V5m-6 6 6-6 6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
                : '<path d="M12 5v14m-6-6 6 6 6-6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>';
            return `<div class="transaction-row"><div class="transaction-main"><span class="transaction-icon ${income ? 'income' : ''}"><svg viewBox="0 0 24 24" fill="none">${icon}</svg></span><div style="min-width:0"><div class="transaction-note" title="${escapeHtml(label)}">${escapeHtml(label)}</div><div class="transaction-category">${escapeHtml(transaction.categoryName || 'Chưa phân loại')}</div></div></div><span class="transaction-wallet">${escapeHtml(transaction.walletName)}</span><span class="transaction-date">${formatDate(transaction.transactionDate)}</span><span class="transaction-amount ${income ? 'income' : 'expense'}">${income ? '+' : '−'}${money.format(transaction.amount)}</span><button class="row-menu" data-delete="${transaction.id}" aria-label="Xóa giao dịch" title="Xóa giao dịch"><svg viewBox="0 0 24 24" fill="none"><path d="M5 7h14M10 11v6m4-6v6M6.5 7l.8 13h9.4l.8-13M9 7V4h6v3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg></button></div>`;
        }).join('');
        setMessage('empty-state', undefined, filtered.length === 0);
    }
    function renderCategories() {
        const totals = new Map();
        for (const transaction of transactions) {
            if (transaction.type !== 'expense') continue;
            const name = transaction.categoryName || 'Chưa phân loại';
            totals.set(name, (totals.get(name) || 0) + Number(transaction.amount));
        }
        const values = [...totals.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5);
        const highest = Math.max(...values.map(([, amount]) => amount), 1);
        const target = document.getElementById('category-list');
        target.innerHTML = values.length
            ? values.map(([name, amount]) => `<div class="category-item"><span class="category-name" title="${escapeHtml(name)}">${escapeHtml(name)}</span><div class="category-track"><div class="category-fill" style="width:${Math.max(5, amount / highest * 100)}%"></div></div><span class="category-amount">${money.format(amount)}</span></div>`).join('')
            : '<div class="budget-caption">Chưa có khoản chi nào để phân tích.</div>';
    }
    async function loadDashboard() {
        setMessage('error-state', '', false);
        setMessage('loading-state', undefined, true);
        try {
            const query = `userId=${USER_ID}&month=${encodeURIComponent(selectedMonth())}`;
            const [summary, rows] = await Promise.all([
                api(`/api/finance/summary?${query}`),
                api(`/api/finance/transactions?${query}`)
            ]);
            transactions = rows;
            document.getElementById('stat-income').textContent = money.format(summary.income);
            document.getElementById('stat-expense').textContent = money.format(summary.expense);
            document.getElementById('stat-balance').textContent = money.format(summary.walletBalance);
            document.getElementById('stat-budget').textContent = money.format(summary.budget);
            document.getElementById('budget-spent').textContent = money.format(summary.expense);
            const percent = summary.budget > 0 ? Math.round(summary.expense / summary.budget * 100) : 0;
            document.getElementById('budget-percent').textContent = summary.budget > 0 ? `${percent}% ngân sách` : 'Chưa đặt ngân sách';
            document.getElementById('budget-fill').style.width = `${Math.min(percent, 100)}%`;
            document.getElementById('budget-caption').textContent = summary.budget > 0
                ? (summary.expense <= summary.budget
                    ? `Bạn còn ${money.format(summary.budget - summary.expense)} trong ngân sách tháng này.`
                    : `Bạn đã vượt ngân sách ${money.format(summary.expense - summary.budget)}. Hãy cân nhắc các khoản chi tiếp theo.`)
                : 'Chưa có ngân sách tháng trong cơ sở dữ liệu.';
            renderTransactions();
            renderCategories();
        } catch (error) {
            setMessage('error-state', `Không tải được dữ liệu. ${error.message}`, true);
        } finally {
            setMessage('loading-state', undefined, false);
        }
    }
    function showToast(message) {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.classList.add('show');
        clearTimeout(showToast.timer);
        showToast.timer = setTimeout(() => toast.classList.remove('show'), 2200);
    }
    async function openDialog() {
        document.getElementById('form-error').classList.remove('visible');
        const [year, month] = selectedMonth().split('-').map(Number);
        const now = new Date();
        const defaultDate = year === now.getFullYear() && month === now.getMonth() + 1
            ? now : new Date(year, month - 1, 1);
        document.getElementById('transaction-date').value = new Date(
            defaultDate.getTime() - defaultDate.getTimezoneOffset() * 60000
        ).toISOString().slice(0, 16);
        try {
            await Promise.all([loadCategories(), loadWallets()]);
            dialog.showModal();
        } catch (error) {
            showToast(`Không tải được danh mục hoặc ví: ${error.message}`);
        }
    }
    document.getElementById('open-form').addEventListener('click', openDialog);
    document.getElementById('close-form').addEventListener('click', () => dialog.close());
    document.getElementById('cancel-form').addEventListener('click', () => dialog.close());
    typeSelect.addEventListener('change', () => loadCategories().catch(error => {
        const fieldError = document.getElementById('form-error');
        fieldError.textContent = `Không tải được danh mục: ${error.message}`;
        fieldError.classList.add('visible');
    }));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const errorElement = document.getElementById('form-error');
        errorElement.classList.remove('visible');
        const payload = {
            userId: USER_ID,
            walletId: Number(walletSelect.value),
            categoryId: categorySelect.value ? Number(categorySelect.value) : null,
            type: typeSelect.value,
            amount: Number(document.getElementById('transaction-amount').value),
            transactionDate: document.getElementById('transaction-date').value,
            note: document.getElementById('transaction-note').value.trim() || null
        };
        try {
            await api('/api/finance/transactions', { method:'POST', body:JSON.stringify(payload) });
            window.dayflowAssistant?.warnExpense(payload.amount);
            dialog.close();
            form.reset();
            await loadDashboard();
            await loadWallets();
            showToast('Đã lưu giao dịch và cập nhật số dư ví');
        } catch (error) {
            errorElement.textContent = error.message;
            errorElement.classList.add('visible');
        }
    });
    document.querySelectorAll('.filter-button').forEach(button => button.addEventListener('click', () => {
        currentFilter = button.dataset.filter;
        document.querySelectorAll('.filter-button').forEach(item => {
            const active = item === button;
            item.classList.toggle('active', active);
            item.setAttribute('aria-selected', String(active));
        });
        renderTransactions();
    }));
    list.addEventListener('click', async event => {
        const button = event.target.closest('[data-delete]');
        if (!button || !window.confirm('Xóa giao dịch này? Số dư ví sẽ được điều chỉnh lại.')) return;
        button.disabled = true;
        try {
            await api(`/api/finance/transactions/${button.dataset.delete}?userId=${USER_ID}`, { method:'DELETE' });
            await loadDashboard();
            await loadWallets();
            showToast('Đã xóa giao dịch');
        } catch (error) {
            button.disabled = false;
            showToast(`Không xóa được giao dịch: ${error.message}`);
        }
    });
    search.addEventListener('input', renderTransactions);
    window.addEventListener('dayflow:expense-updated', () => { if (USER_ID) loadDashboard(); });
    monthPicker.addEventListener('change', () => {
        const [year, month] = selectedMonth().split('-').map(Number);
        document.getElementById('month-label').textContent = new Intl.DateTimeFormat('vi-VN', {
            month:'long', year:'numeric'
        }).format(new Date(year, month - 1, 1)) + ' · Theo dõi dòng tiền của bạn.';
        loadDashboard();
    });
    const currentMonth = new Date();
    monthPicker.value = `${currentMonth.getFullYear()}-${String(currentMonth.getMonth() + 1).padStart(2, '0')}`;
    document.getElementById('month-label').textContent = new Intl.DateTimeFormat('vi-VN', { month:'long', year:'numeric' }).format(currentMonth) + ' · Theo dõi dòng tiền của bạn.';
    api('/api/auth/me').then(user => {
        USER_ID = user.id;
        const profileButton = document.getElementById('profile-logout');
        profileButton.title = `Đăng xuất · ${user.email}`;
        profileButton.textContent = user.fullName.trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase();
        const logout = async () => {
            await fetch('/api/auth/csrf', {credentials:'same-origin'});
            const cookie = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='));
            await fetch('/api/auth/logout', {method:'POST',credentials:'same-origin',headers:{'X-XSRF-TOKEN':cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : ''}});
            location.href = '/login/index.html';
        };
        document.getElementById('logout-button').addEventListener('click', logout);
        loadDashboard();
    }).catch(() => {
        location.href = `/login/index.html?next=${encodeURIComponent(location.pathname)}`;
    });
})()
