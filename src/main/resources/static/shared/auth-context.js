(() => {
    const KEY = 'dayflow-auth-token';
    const originalFetch = window.fetch.bind(window);
    window.fetch = async (input, init = {}) => {
        const url = typeof input === 'string' ? input : input.url;
        const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
        const token = sessionStorage.getItem(KEY);
        const parsed = new URL(url, location.href);
        if (token && parsed.origin === location.origin && parsed.pathname.startsWith('/api/')) headers.set('Authorization', `Bearer ${token}`);
        const response = await originalFetch(input, {...init, headers});
        if (parsed.pathname === '/api/auth/login' && response.ok) {
            const body = await response.clone().json().catch(() => null);
            if (body?.token) sessionStorage.setItem(KEY, body.token);
        } else if (parsed.pathname === '/api/auth/logout' && response.ok) sessionStorage.removeItem(KEY);
        return response;
    };
})();
