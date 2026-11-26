(function () {
    'use strict';

    // 가맹점 제보(가맹점 상세의 원터치 버튼 + 제보 현황 표시).
    // 지도 쪽(map.js)과는 window.Reports 와 'reports:changed' 이벤트로만 연결한다.

    var TYPES = [
        { type: 'PAYMENT_REFUSED', button: '결제 안 돼요', badge: '결제 안 됨' },
        { type: 'CLOSED', button: '폐업했어요', badge: '폐업' },
        { type: 'WRONG_INFO', button: '정보가 달라요', badge: '정보 다름' }
    ];

    var mine = {}; // "가맹점id:종류" -> true (내가 남긴 제보)
    var fresh = {}; // 가맹점 id -> 방금 제보/취소해서 받은 현황 (다음 지도 조회 전까지 조회 결과보다 최신)
    var pending = {}; // "가맹점id:종류" -> true (요청 중)
    var detailStore = null;

    function el(id) {
        return document.getElementById(id);
    }

    function isLoggedIn() {
        return !!window.currentUser;
    }

    function loginRequired() {
        if (confirm('제보하려면 로그인이 필요합니다. 카카오로 로그인할까요?')) {
            window.location.href = '/oauth2/authorization/kakao';
        }
    }

    function key(storeId, type) {
        return storeId + ':' + type;
    }

    function summaryFor(store) {
        return fresh[store.id] || store.reports || { counts: {}, total: 0, lastReportedAt: null };
    }

    /** 목록/마커에 쓰는 한 줄 요약. 제보가 없으면 빈 문자열. */
    function shortText(store) {
        var counts = summaryFor(store).counts;
        return TYPES.filter(function (t) {
            return counts[t.type] > 0;
        })
            .map(function (t) {
                return t.badge + ' ' + counts[t.type];
            })
            .join(' · ');
    }

    function formatDate(iso) {
        var date = new Date(iso);
        return date.getFullYear() + '.' + (date.getMonth() + 1) + '.' + date.getDate();
    }

    function renderDetail(store) {
        detailStore = store;
        var box = el('detailReports');
        if (!box) return;
        box.textContent = '';
        if (!store) return;

        var summary = summaryFor(store);

        var status = document.createElement('div');
        status.className = 'report-status' + (summary.total > 0 ? ' has-reports' : '');
        status.textContent =
            summary.total > 0
                ? '⚠ 이용자 제보 ' + summary.total + '건 (최근 ' + formatDate(summary.lastReportedAt) + ')'
                : '아직 이용자 제보가 없습니다.';
        box.appendChild(status);

        var buttons = document.createElement('div');
        buttons.className = 'report-buttons';
        TYPES.forEach(function (t) {
            var reported = !!mine[key(store.id, t.type)];
            var count = summary.counts[t.type] || 0;

            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'report-btn' + (reported ? ' reported' : '');
            button.setAttribute('aria-pressed', reported);
            button.disabled = !!pending[key(store.id, t.type)];
            button.appendChild(document.createTextNode((reported ? '✓ ' : '') + t.button));
            var countEl = document.createElement('span');
            countEl.className = 'report-count';
            countEl.textContent = count;
            button.appendChild(countEl);
            button.addEventListener('click', function () {
                toggle(store, t.type);
            });
            buttons.appendChild(button);
        });
        box.appendChild(buttons);

        var hint = document.createElement('div');
        hint.className = 'report-hint';
        hint.textContent = '버튼을 누르면 바로 제보되고, 다시 누르면 취소됩니다.';
        box.appendChild(hint);
    }

    /** 요청 중 다른 가맹점을 열었을 수 있으므로 지금 열려 있는 상세를 다시 그린다. */
    function refreshDetail() {
        renderDetail(detailStore);
    }

    function toggle(store, type) {
        if (!isLoggedIn()) {
            loginRequired();
            return;
        }
        var k = key(store.id, type);
        if (pending[k]) return;
        var add = !mine[k];
        pending[k] = true;
        refreshDetail();

        window
            .authFetch('/api/me/stores/' + store.id + '/reports/' + type, { method: add ? 'PUT' : 'DELETE' })
            .then(function (res) {
                if (res.status === 401) {
                    loginRequired();
                    return;
                }
                return res
                    .json()
                    .catch(function () {
                        return {};
                    })
                    .then(function (data) {
                        if (!res.ok) throw new Error(data.message || '제보를 처리하지 못했습니다.');
                        if (add) mine[k] = true;
                        else delete mine[k];
                        fresh[store.id] = data;
                        document.dispatchEvent(new CustomEvent('reports:changed'));
                    });
            })
            .catch(function (err) {
                alert(err.message || '제보를 처리하지 못했습니다.');
            })
            .then(function () {
                delete pending[k];
                refreshDetail();
            });
    }

    /** 서버에서 새로 받은 가맹점은 그 응답의 제보 현황이 가장 최신이므로 임시 값을 버린다. */
    function absorb(stores) {
        stores.forEach(function (store) {
            delete fresh[store.id];
        });
    }

    function loadMine() {
        if (!isLoggedIn()) {
            mine = {};
            refreshDetail();
            return;
        }
        window
            .authFetch('/api/me/reports')
            .then(function (res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function (reports) {
                mine = {};
                reports.forEach(function (r) {
                    mine[key(r.storeId, r.type)] = true;
                });
                refreshDetail();
            })
            .catch(function (err) {
                console.error('내 제보 조회 실패', err);
            });
    }

    function init() {
        document.addEventListener('auth:changed', loadMine);
        // auth.js가 이 스크립트보다 먼저 로그인 확인을 끝냈다면 이벤트를 놓쳤으므로 현재 상태로 한 번 맞춘다.
        if (isLoggedIn()) loadMine();
    }

    window.Reports = {
        renderDetail: renderDetail,
        totalFor: function (store) {
            return summaryFor(store).total;
        },
        shortText: shortText,
        absorb: absorb
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
