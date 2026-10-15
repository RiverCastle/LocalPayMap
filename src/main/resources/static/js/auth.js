(function () {
    'use strict';

    // 로그인 상태는 HttpOnly 쿠키라 JS에서 직접 읽을 수 없으므로 /api/me로 확인한다.
    // 지도 조회는 로그인이 필요 없으므로 이 스크립트가 실패해도 지도는 정상 동작한다.
    window.currentUser = null;

    function readCookie(name) {
        var m = document.cookie.match('(?:^|; )' + name + '=([^;]*)');
        return m ? decodeURIComponent(m[1]) : null;
    }

    // 로그인이 필요한 API 호출에 쓰는 공통 fetch: CSRF 헤더 포함, 401이면 refresh 후 1회 재시도.
    window.authFetch = function (url, options) {
        options = Object.assign({ credentials: 'same-origin' }, options || {});
        options.headers = Object.assign({}, options.headers);
        var token = readCookie('XSRF-TOKEN');
        if (token) options.headers['X-XSRF-TOKEN'] = token;
        return fetch(url, options).then(function (res) {
            if (res.status !== 401) return res;
            return refresh().then(function (ok) {
                return ok ? fetch(url, options) : res;
            });
        });
    };

    function refresh() {
        var headers = {};
        var token = readCookie('XSRF-TOKEN');
        if (token) headers['X-XSRF-TOKEN'] = token;
        return fetch('/api/auth/refresh', { method: 'POST', headers: headers, credentials: 'same-origin' })
            .then(function (res) { return res.ok; })
            .catch(function () { return false; });
    }

    function loadMe() {
        return fetch('/api/me', { credentials: 'same-origin' }).then(function (r) { return r.json(); });
    }

    function render(me) {
        var area = document.getElementById('authArea');
        if (!area) return;
        window.currentUser = me && me.authenticated ? me : null;
        area.textContent = '';
        if (window.currentUser) {
            var name = document.createElement('span');
            name.textContent = window.currentUser.nickname + '님';
            var out = document.createElement('button');
            out.type = 'button';
            out.className = 'auth-btn secondary';
            out.textContent = '로그아웃';
            out.addEventListener('click', logout);
            area.appendChild(name);
            area.appendChild(out);
        } else {
            var login = document.createElement('a');
            login.className = 'auth-btn';
            login.href = '/oauth2/authorization/kakao';
            login.textContent = '카카오 로그인';
            area.appendChild(login);
        }
        document.dispatchEvent(new CustomEvent('auth:changed', { detail: window.currentUser }));
    }

    function logout() {
        window.authFetch('/api/auth/logout', { method: 'POST' }).then(function () {
            render(null);
        });
    }

    function init() {
        loadMe()
            .then(function (me) {
                if (me.authenticated) return me;
                // access 토큰만 만료됐을 수 있으므로 refresh를 한 번 시도한다. (없으면 401로 조용히 끝난다)
                return refresh().then(function (ok) { return ok ? loadMe() : me; });
            })
            .then(render)
            .catch(function () { render(null); });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
