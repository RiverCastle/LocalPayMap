(function () {
    var PAGE_SIZE = 20;
    var page = 0;

    function el(id) {
        return document.getElementById(id);
    }

    function readCookie(name) {
        var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
        return match ? decodeURIComponent(match[2]) : null;
    }

    function authHeaders() {
        var headers = { 'Content-Type': 'application/json' };
        var token = readCookie('XSRF-TOKEN');
        if (token) headers[window.CSRF_HEADER_NAME || 'X-XSRF-TOKEN'] = token;
        return headers;
    }

    function text(tag, content) {
        var node = document.createElement(tag);
        node.textContent = content == null ? '' : content;
        return node;
    }

    // 그룹의 첫 번째(가장 먼저 등록된, id 최소) 가맹점은 유지하고 나머지를 체크한다.
    function selectAllButFirst() {
        document.querySelectorAll('.dup-group').forEach(function (group) {
            group.querySelectorAll('input.dup-check').forEach(function (box, i) {
                box.checked = i > 0;
            });
        });
    }

    function render(data) {
        var totalGroups = data.totalGroups;
        var container = el('dupGroups');
        container.innerHTML = '';
        el('dupSummary').textContent = '중복 그룹 총 ' + totalGroups + '개 ';
        var lastPage = Math.max(0, Math.ceil(totalGroups / PAGE_SIZE) - 1);
        el('pageInfo').textContent = page + 1 + ' / ' + (lastPage + 1);
        el('prevPage').disabled = page <= 0;
        el('nextPage').disabled = page >= lastPage;

        if (!data.groups.length) {
            container.appendChild(text('p', '중복된 가맹점이 없습니다.'));
            return;
        }

        data.groups.forEach(function (group) {
            var box = document.createElement('div');
            box.className = 'dup-group';
            box.style.cssText = 'margin:16px 0';

            var title = text('h3', group.name + ' (' + group.stores.length + '건)');
            title.style.cssText = 'margin:0 0 4px;font-size:15px';
            box.appendChild(title);
            box.appendChild(text('div', group.roadAddress));

            var table = document.createElement('table');
            table.innerHTML =
                '<thead><tr><th>삭제</th><th>ID</th><th>지역화폐</th><th>전화번호</th><th>업종</th><th>구분</th><th>지번주소</th></tr></thead>';
            var tbody = document.createElement('tbody');
            group.stores.forEach(function (store) {
                var tr = document.createElement('tr');
                var checkTd = document.createElement('td');
                var check = document.createElement('input');
                check.type = 'checkbox';
                check.className = 'dup-check';
                check.value = store.id;
                checkTd.appendChild(check);
                tr.appendChild(checkTd);
                tr.appendChild(text('td', store.id));
                tr.appendChild(text('td', store.currencyName || ''));
                tr.appendChild(text('td', store.phone || ''));
                tr.appendChild(text('td', store.category || ''));
                tr.appendChild(text('td', store.bizStatus === 'OPEN' ? '영업중' : '폐업'));
                tr.appendChild(text('td', store.jibunAddress || ''));
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            box.appendChild(table);
            container.appendChild(box);
        });
        selectAllButFirst();
    }

    function load() {
        fetch('/admin/api/stores/duplicates?page=' + page + '&size=' + PAGE_SIZE, { headers: authHeaders() })
            .then(function (res) {
                if (!res.ok) throw new Error('조회 실패 (HTTP ' + res.status + ')');
                return res.json();
            })
            .then(function (data) {
                // 삭제 후 현재 페이지가 비면 이전 페이지로 이동
                if (!data.groups.length && page > 0) {
                    page -= 1;
                    return load();
                }
                render(data);
            })
            .catch(function (err) {
                el('dupStatus').textContent = err.message;
            });
    }

    function deleteSelected() {
        var ids = [];
        var unsafe = false;
        document.querySelectorAll('.dup-group').forEach(function (group) {
            var boxes = group.querySelectorAll('input.dup-check');
            var checked = group.querySelectorAll('input.dup-check:checked');
            if (checked.length === boxes.length) unsafe = true;
            checked.forEach(function (box) {
                ids.push(Number(box.value));
            });
        });
        if (!ids.length) {
            el('dupStatus').textContent = '선택된 항목이 없습니다.';
            return;
        }
        var message = ids.length + '건을 삭제할까요?';
        if (unsafe) message += '\n※ 일부 그룹은 모든 항목이 선택되어 해당 가맹점이 완전히 사라집니다.';
        if (!confirm(message)) return;

        el('dupStatus').textContent = '삭제 중...';
        fetch('/admin/api/stores/duplicates/delete', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ ids: ids })
        })
            .then(function (res) {
                if (!res.ok) throw new Error('삭제 실패 (HTTP ' + res.status + ')');
                return res.json();
            })
            .then(function (result) {
                el('dupStatus').textContent = result.deletedCount + '건 삭제되었습니다.';
                load();
            })
            .catch(function (err) {
                el('dupStatus').textContent = err.message;
            });
    }

    document.addEventListener('DOMContentLoaded', function () {
        el('autoSelectAll').addEventListener('click', selectAllButFirst);
        el('deleteSelected').addEventListener('click', deleteSelected);
        el('prevPage').addEventListener('click', function () {
            page = Math.max(0, page - 1);
            load();
        });
        el('nextPage').addEventListener('click', function () {
            page += 1;
            load();
        });
        load();
    });
})();
