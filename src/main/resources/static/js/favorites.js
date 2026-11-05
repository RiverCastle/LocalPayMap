(function () {
    'use strict';

    // 즐겨찾기 그룹 관리(패널 + 가맹점 상세의 "그룹에 담기").
    // 지도 쪽(map.js)과는 window.Favorites 와 'favorites:changed' / 'favorites:fit' 이벤트로만 연결한다.
    // 사용자가 입력한 그룹 이름은 항상 textContent 로만 화면에 넣는다. (XSS 방지)

    var API = '/api/me/favorite-groups';
    var DEFAULT_COLOR = '#2f6fed';

    var groups = [];
    var visible = {}; // 지도에 표시 중인 그룹 id -> true
    var groupStores = {}; // 그룹 id -> 가맹점 목록 캐시 (지도에 표시 중인 그룹만 채워진다)
    var colorByStore = {}; // 가맹점 id -> 마커 색 (여러 그룹에 담겼으면 먼저 만든 그룹의 색)
    var detailStore = null;

    function el(id) {
        return document.getElementById(id);
    }

    function emit(name, detail) {
        document.dispatchEvent(new CustomEvent(name, { detail: detail }));
    }

    function loginRequired() {
        if (confirm('로그인이 필요한 기능입니다. 카카오로 로그인할까요?')) {
            window.location.href = '/oauth2/authorization/kakao';
        }
    }

    function isLoggedIn() {
        return !!window.currentUser;
    }

    /** 서버 호출 공통부: 실패하면 서버가 준 message 로 Error 를 던진다. */
    function api(method, url, body) {
        var options = { method: method, headers: {} };
        if (body !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        return window.authFetch(url, options).then(function (res) {
            if (res.status === 401) {
                loginRequired();
                throw new Error('');
            }
            if (!res.ok) {
                return res
                    .json()
                    .catch(function () {
                        return {};
                    })
                    .then(function (data) {
                        throw new Error(data.message || '요청을 처리하지 못했습니다.');
                    });
            }
            return res.status === 204 ? null : res.json();
        });
    }

    function fail(err) {
        if (err && err.message) alert(err.message);
    }

    function findGroup(id) {
        return groups.filter(function (g) {
            return g.id === id;
        })[0];
    }

    function rebuildColorIndex() {
        colorByStore = {};
        // 늦게 만든 그룹부터 덮어써서, 최종적으로는 먼저 만든 그룹의 색이 남는다.
        groups
            .slice()
            .reverse()
            .forEach(function (g) {
                g.storeIds.forEach(function (storeId) {
                    colorByStore[storeId] = g.color;
                });
            });
    }

    function visibleCount() {
        return Object.keys(visible).length;
    }

    function changed() {
        rebuildColorIndex();
        renderPanel();
        renderDetail(detailStore);
        emit('favorites:changed');
    }

    // ---------- 데이터 로딩 ----------

    function loadGroupStores(groupId) {
        return api('GET', API + '/' + groupId + '/stores').then(function (stores) {
            groupStores[groupId] = stores;
        });
    }

    function load() {
        return api('GET', API)
            .then(function (list) {
                groups = list;
                Object.keys(visible).forEach(function (id) {
                    if (!findGroup(Number(id))) delete visible[id];
                });
                return Promise.all(
                    Object.keys(visible).map(function (id) {
                        return loadGroupStores(Number(id));
                    })
                );
            })
            .then(changed)
            .catch(fail);
    }

    function reset() {
        groups = [];
        visible = {};
        groupStores = {};
        el('favoritePanel').classList.add('hidden');
        changed();
    }

    // ---------- 그룹 변경 ----------

    function createGroup(name) {
        return api('POST', API, { name: name }).then(function (group) {
            groups.push(group);
            changed();
            return group;
        });
    }

    function renameGroup(group) {
        var name = prompt('그룹 이름', group.name);
        if (name === null) return;
        api('PATCH', API + '/' + group.id, { name: name, color: group.color })
            .then(function (updated) {
                group.name = updated.name;
                changed();
            })
            .catch(fail);
    }

    function recolorGroup(group, color) {
        api('PATCH', API + '/' + group.id, { name: group.name, color: color })
            .then(function (updated) {
                group.color = updated.color;
                changed();
            })
            .catch(fail);
    }

    function deleteGroup(group) {
        if (!confirm("'" + group.name + "' 그룹을 삭제할까요? 담아 둔 가맹점 목록도 함께 사라집니다.")) return;
        api('DELETE', API + '/' + group.id)
            .then(function () {
                groups = groups.filter(function (g) {
                    return g.id !== group.id;
                });
                delete visible[group.id];
                delete groupStores[group.id];
                changed();
            })
            .catch(fail);
    }

    function setStoreInGroup(group, store, add) {
        return api(add ? 'PUT' : 'DELETE', API + '/' + group.id + '/stores/' + store.id).then(function () {
            if (add) {
                if (group.storeIds.indexOf(store.id) < 0) group.storeIds.push(store.id);
            } else {
                group.storeIds = group.storeIds.filter(function (id) {
                    return id !== store.id;
                });
            }
            // 지도에 표시 중인 그룹이면 표시 목록도 맞춰 갱신한다.
            if (visible[group.id]) return loadGroupStores(group.id);
        });
    }

    function toggleVisible(group) {
        if (visible[group.id]) {
            delete visible[group.id];
            changed();
            return;
        }
        loadGroupStores(group.id)
            .then(function () {
                visible[group.id] = true;
                changed();
                if (groupStores[group.id].length > 0) emit('favorites:fit', visibleStores());
            })
            .catch(fail);
    }

    function clearVisible() {
        visible = {};
        changed();
    }

    // ---------- 지도 연동용 조회 ----------

    function visibleStores() {
        var seen = {};
        var result = [];
        Object.keys(visible).forEach(function (id) {
            (groupStores[id] || []).forEach(function (store) {
                if (!seen[store.id]) {
                    seen[store.id] = true;
                    result.push(store);
                }
            });
        });
        return result;
    }

    // ---------- 화면: 패널 ----------

    function button(text, className, onClick) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = className;
        b.textContent = text;
        b.addEventListener('click', onClick);
        return b;
    }

    function renderPanel() {
        var list = el('favoriteGroupList');
        if (!list) return;
        list.textContent = '';

        if (groups.length === 0) {
            var empty = document.createElement('p');
            empty.className = 'fav-empty';
            empty.textContent = '아직 그룹이 없습니다. 아래에서 첫 그룹을 만들어 보세요.';
            list.appendChild(empty);
        }

        groups.forEach(function (group) {
            var row = document.createElement('div');
            row.className = 'fav-group-row';

            var color = document.createElement('input');
            color.type = 'color';
            color.className = 'fav-color';
            color.value = group.color;
            color.title = '마커 색상';
            color.addEventListener('change', function () {
                recolorGroup(group, color.value);
            });

            var info = document.createElement('div');
            info.className = 'fav-group-info';
            var name = document.createElement('strong');
            name.textContent = group.name;
            var count = document.createElement('span');
            count.textContent = group.storeIds.length + '곳';
            info.appendChild(name);
            info.appendChild(count);

            var show = document.createElement('label');
            show.className = 'fav-show';
            var check = document.createElement('input');
            check.type = 'checkbox';
            check.checked = !!visible[group.id];
            check.addEventListener('change', function () {
                toggleVisible(group);
            });
            show.appendChild(check);
            show.appendChild(document.createTextNode('지도에 표시'));

            var actions = document.createElement('div');
            actions.className = 'fav-actions';
            actions.appendChild(button('이름 변경', 'fav-link', function () { renameGroup(group); }));
            actions.appendChild(button('삭제', 'fav-link danger', function () { deleteGroup(group); }));

            row.appendChild(color);
            row.appendChild(info);
            row.appendChild(show);
            row.appendChild(actions);
            list.appendChild(row);
        });

        var status = el('favoriteFilterStatus');
        var count = visibleCount();
        status.classList.toggle('hidden', count === 0);
        el('favoriteFilterText').textContent = '선택한 ' + count + '개 그룹의 가맹점만 지도에 표시 중입니다.';
    }

    function submitNewGroup() {
        var input = el('newGroupName');
        var name = input.value.trim();
        if (!name) return;
        createGroup(name)
            .then(function () {
                input.value = '';
            })
            .catch(fail);
    }

    // ---------- 화면: 가맹점 상세의 "그룹에 담기" ----------

    function renderDetail(store) {
        detailStore = store;
        var box = el('detailGroups');
        if (!box) return;
        box.textContent = '';
        if (!store) return;

        if (!isLoggedIn()) {
            box.appendChild(button('☆ 즐겨찾기 그룹에 담기', 'fav-add-btn', loginRequired));
            return;
        }

        var title = document.createElement('div');
        title.className = 'fav-detail-title';
        title.textContent = '★ 내 즐겨찾기 그룹에 담기';
        box.appendChild(title);

        groups.forEach(function (group) {
            var label = document.createElement('label');
            label.className = 'fav-detail-row';
            var check = document.createElement('input');
            check.type = 'checkbox';
            check.checked = group.storeIds.indexOf(store.id) >= 0;
            check.addEventListener('change', function () {
                var add = check.checked;
                check.disabled = true;
                setStoreInGroup(group, store, add)
                    .then(changed)
                    .catch(function (err) {
                        check.checked = !add;
                        fail(err);
                    })
                    .then(function () {
                        check.disabled = false;
                    });
            });
            var dot = document.createElement('span');
            dot.className = 'fav-dot';
            dot.style.background = group.color;
            label.appendChild(check);
            label.appendChild(dot);
            label.appendChild(document.createTextNode(group.name));
            box.appendChild(label);
        });

        var form = document.createElement('div');
        form.className = 'fav-detail-new';
        var input = document.createElement('input');
        input.type = 'text';
        input.maxLength = 50;
        input.placeholder = groups.length === 0 ? '새 그룹 이름 (예: 데이트 맛집)' : '새 그룹 만들고 담기';
        var add = button('만들고 담기', 'fav-link', function () {
            var name = input.value.trim();
            if (!name) return;
            createGroup(name)
                .then(function (group) {
                    return setStoreInGroup(group, store, true);
                })
                .then(changed)
                .catch(fail);
        });
        form.appendChild(input);
        form.appendChild(add);
        box.appendChild(form);
    }

    // ---------- 초기화 ----------

    function init() {
        el('favoritePanelButton').addEventListener('click', function () {
            if (!isLoggedIn()) {
                loginRequired();
                return;
            }
            el('favoritePanel').classList.toggle('hidden');
        });
        el('closeFavoritePanel').addEventListener('click', function () {
            el('favoritePanel').classList.add('hidden');
        });
        el('addGroupButton').addEventListener('click', submitNewGroup);
        el('newGroupName').addEventListener('keydown', function (event) {
            if (event.key === 'Enter') submitNewGroup();
        });
        el('clearFavoriteFilter').addEventListener('click', clearVisible);

        document.addEventListener('auth:changed', function () {
            if (isLoggedIn()) load();
            else reset();
        });
        // auth.js가 이 스크립트보다 먼저 로그인 확인을 끝냈다면 이벤트를 놓쳤으므로 현재 상태로 한 번 맞춘다.
        if (isLoggedIn()) load();
        renderPanel();
    }

    window.Favorites = {
        colorFor: function (storeId) {
            return colorByStore[storeId] || null;
        },
        defaultColor: DEFAULT_COLOR,
        isFilterActive: function () {
            return visibleCount() > 0;
        },
        visibleStores: visibleStores,
        renderDetail: renderDetail
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
