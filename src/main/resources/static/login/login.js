(() => {
    const $ = selector => document.querySelector(selector);
    const form = $('#auth-form');
    const registering = form.dataset.mode === 'register';
    const nameInput = $('#name');            // chỉ có ở trang đăng ký
    const emailInput = $('#email');
    const passwordInput = $('#password');
    const errorBox = $('#error');
    const submitButton = $('#submit');
    const mascot = $('#mascot');
    const sadPose = mascot.querySelector('.pose-sad');

    const next = new URLSearchParams(location.search).get('next');
    const destination = role => next && next.startsWith('/') && !next.startsWith('//')
        ? next
        : role === 'ADMIN' ? '/admin/index.html' : '/todo/index.html';

    // ---------- Linh vật: ngồi im <-> buồn ----------
    function setSad(sad) {
        const changed = mascot.classList.contains('sad') !== sad;
        mascot.classList.toggle('sad', sad);
        mascot.setAttribute('aria-label', sad
            ? 'Linh vật đang buồn vì bạn nhập chưa đúng'
            : registering ? 'Linh vật đang ngồi chờ bạn đăng ký' : 'Linh vật đang ngồi chờ bạn đăng nhập');
        if (sad) {                       // lắc đầu mỗi lần sai, kể cả khi đang buồn sẵn
            sadPose.classList.remove('shake');
            void sadPose.offsetWidth;
            sadPose.classList.add('shake');
        } else if (changed) {
            sadPose.classList.remove('shake');
        }
    }

    const inputs = [nameInput, emailInput, passwordInput].filter(Boolean);

    function showError(message, invalidFields = []) {
        errorBox.textContent = message;
        errorBox.classList.add('show');
        inputs.forEach(input =>
            input.setAttribute('aria-invalid', invalidFields.includes(input) ? 'true' : 'false'));
        setSad(true);
        if (invalidFields[0]) invalidFields[0].focus();
    }

    function clearError() {
        errorBox.classList.remove('show');
        inputs.forEach(input => input.removeAttribute('aria-invalid'));
        setSad(false);
    }

    // Gõ lại là linh vật vui trở lại
    inputs.forEach(input => input.addEventListener('input', () => {
        if (mascot.classList.contains('sad')) clearError();
    }));

    // ---------- Gọi API (giữ nguyên cơ chế CSRF cũ) ----------
    function csrfCookie() {
        const item = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='));
        return item ? decodeURIComponent(item.slice('XSRF-TOKEN='.length)) : '';
    }
    async function prepareCsrf() {
        await fetch('/api/auth/csrf', {credentials: 'same-origin'});
        return csrfCookie();
    }
    async function request(path, body) {
        const token = await prepareCsrf();
        const response = await fetch(path, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json', 'X-XSRF-TOKEN': token},
            body: JSON.stringify(body)
        });
        const value = await response.json().catch(() => ({}));
        if (!response.ok) {
            const fallback = response.status === 401
                ? 'Email hoặc mật khẩu không đúng'
                : `Yêu cầu thất bại (${response.status})`;
            const failure = new Error(value.detail || value.message || fallback);
            failure.status = response.status;
            throw failure;
        }
        return value;
    }

    // ---------- Gửi form ----------
    const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    form.addEventListener('submit', async event => {
        event.preventDefault();
        clearError();

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        // Thiếu thông tin
        const fullName = nameInput ? nameInput.value.trim() : '';
        const missing = [];
        if (nameInput && !fullName) missing.push(nameInput);
        if (!email) missing.push(emailInput);
        if (!password) missing.push(passwordInput);
        if (missing.length) {
            const label = {name: 'họ tên', email: 'email', password: 'mật khẩu'};
            const names = missing.map(input => label[input.id]);
            const what = names.length > 1 ? names.slice(0, -1).join(', ') + ' và ' + names.at(-1) : names[0];
            showError(`Bạn chưa nhập ${what}.`, missing);
            return;
        }
        // Sai định dạng email
        if (!EMAIL_PATTERN.test(email)) {
            showError('Email chưa đúng định dạng.', [emailInput]);
            return;
        }

        if (registering && password.length < 6) {
            showError('Mật khẩu cần ít nhất 6 ký tự.', [passwordInput]);
            return;
        }

        submitButton.disabled = true;
        try {
            if (registering) await request('/api/auth/register', {email, password, fullName});
            const user = await request('/api/auth/login', {email, password});
            await prepareCsrf();
            location.href = destination(user.role);
        } catch (error) {
            const offline = error instanceof TypeError;
            const duplicate = registering && error.status === 409;
            showError(offline ? 'Không kết nối được máy chủ. Vui lòng thử lại.' : error.message,
                offline ? [] : [duplicate ? emailInput : passwordInput]);
        } finally {
            submitButton.disabled = false;
        }
    });

})();
