(function () {
    function el(id) {
        return document.getElementById(id);
    }

    function readCookie(name) {
        var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
        return match ? decodeURIComponent(match[2]) : null;
    }

    function authHeaders(extra) {
        var headers = Object.assign({ 'Content-Type': 'application/json' }, extra || {});
        var token = readCookie('XSRF-TOKEN');
        if (token) headers[window.CSRF_HEADER_NAME || 'X-XSRF-TOKEN'] = token;
        return headers;
    }

    function csrfOnlyHeaders() {
        var headers = {};
        var token = readCookie('XSRF-TOKEN');
        if (token) headers[window.CSRF_HEADER_NAME || 'X-XSRF-TOKEN'] = token;
        return headers;
    }

    function loadCurrencyTypes() {
        fetch('/api/currency-types')
            .then(function (res) {
                return res.json();
            })
            .then(function (types) {
                var select = el('formCurrencyTypeId');
                while (select.options.length > 1) {
                    select.remove(1);
                }
                types.forEach(function (type) {
                    var option = document.createElement('option');
                    option.value = type.id;
                    option.textContent = type.regionName + ' - ' + type.currencyName;
                    select.appendChild(option);
                });
            });
    }

    function loadStores() {
        fetch('/admin/api/stores?size=200', { headers: authHeaders() })
            .then(function (res) {
                return res.json();
            })
            .then(function (stores) {
                var tbody = el('storeTable').querySelector('tbody');
                tbody.innerHTML = '';
                stores.forEach(function (store) {
                    var tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>' + escapeHtml(store.name) + '</td>' +
                        '<td>' + escapeHtml(store.roadAddress || '') + '</td>' +
                        '<td>' + escapeHtml(store.currencyName || '') + '</td>' +
                        '<td>' + (store.bizStatus === 'OPEN' ? '영업중' : '폐업') + '</td>' +
                        '<td></td>';

                    var editBtn = document.createElement('button');
                    editBtn.type = 'button';
                    editBtn.textContent = '수정';
                    editBtn.addEventListener('click', function () {
                        fillForm(store);
                    });

                    var deleteBtn = document.createElement('button');
                    deleteBtn.type = 'button';
                    deleteBtn.textContent = '삭제';
                    deleteBtn.addEventListener('click', function () {
                        if (!confirm(store.name + ' 가맹점을 삭제할까요?')) return;
                        fetch('/admin/api/stores/' + store.id, { method: 'DELETE', headers: authHeaders() }).then(
                            function () {
                                loadStores();
                            }
                        );
                    });

                    var actionsTd = tr.querySelector('td:last-child');
                    actionsTd.appendChild(editBtn);
                    actionsTd.appendChild(deleteBtn);
                    tbody.appendChild(tr);
                });
            });
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function fillForm(store) {
        el('storeId').value = store.id;
        el('formName').value = store.name;
        el('formCurrencyTypeId').value = store.currencyTypeId || '';
        el('formRoadAddress').value = store.roadAddress || '';
        el('formJibunAddress').value = store.jibunAddress || '';
        el('formPhone').value = store.phone || '';
        el('formCategory').value = store.category || '';
        el('formBizStatus').value = store.bizStatus;
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function resetForm() {
        el('storeId').value = '';
        el('storeForm').reset();
    }

    function loadSyncLogs() {
        fetch('/admin/api/sync/logs', { headers: authHeaders() })
            .then(function (res) {
                return res.json();
            })
            .then(function (logs) {
                var tbody = el('syncLogTable').querySelector('tbody');
                tbody.innerHTML = '';
                logs.forEach(function (log) {
                    var tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>' + log.executedAt + '</td>' +
                        '<td>' + log.status + '</td>' +
                        '<td>' + log.insertedCount + '</td>' +
                        '<td>' + log.updatedCount + '</td>' +
                        '<td>' + escapeHtml(log.message || '') + '</td>';
                    tbody.appendChild(tr);
                });
            });
    }

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrencyTypes();
        loadStores();
        loadSyncLogs();

        el('syncButton').addEventListener('click', function () {
            el('syncStatus').textContent = '동기화 중...';
            fetch('/admin/api/sync/open-data', { method: 'POST', headers: authHeaders() })
                .then(function (res) {
                    return res.json();
                })
                .then(function () {
                    el('syncStatus').textContent = '';
                    loadSyncLogs();
                    loadStores();
                })
                .catch(function () {
                    el('syncStatus').textContent = '동기화 실패';
                });
        });

        el('importForm').addEventListener('submit', function (event) {
            event.preventDefault();
            var fileInput = el('importFile');
            if (!fileInput.files.length) return;

            var formData = new FormData();
            formData.append('file', fileInput.files[0]);

            // regionName/currencyName은 멀티파트 폼 필드 대신 쿼리스트링으로 보낸다.
            // (멀티파트 텍스트 필드는 컨테이너가 한글을 깨뜨리는 경우가 있어, UTF-8로 안정적으로 디코딩되는
            //  URL 쿼리 파라미터 쪽을 사용)
            var params = new URLSearchParams({
                regionName: el('importRegionName').value,
                currencyName: el('importCurrencyName').value
            });

            el('importStatus').textContent = '업로드 및 가져오는 중... (대용량 파일은 시간이 걸릴 수 있습니다)';
            fetch('/admin/api/stores/import?' + params.toString(), {
                method: 'POST',
                headers: csrfOnlyHeaders(),
                body: formData
            })
                .then(function (res) {
                    if (!res.ok) throw new Error('가져오기 실패 (HTTP ' + res.status + ')');
                    return res.json();
                })
                .then(function (result) {
                    el('importStatus').textContent =
                        result.status + ' - 신규 ' + result.insertedCount + '건, 수정 ' + result.updatedCount + '건';
                    loadSyncLogs();
                    loadStores();
                    loadCurrencyTypes();
                })
                .catch(function (err) {
                    el('importStatus').textContent = err.message;
                });
        });

        el('stagingImportButton').addEventListener('click', function () {
            el('stagingImportStatus').textContent = '가져오는 중... (수만 건이라 시간이 걸릴 수 있습니다)';
            fetch('/admin/api/stores/import-suwon-staging', {
                method: 'POST',
                headers: csrfOnlyHeaders()
            })
                .then(function (res) {
                    return res.json().then(function (body) {
                        if (!res.ok) throw new Error(body.message || '가져오기 실패 (HTTP ' + res.status + ')');
                        return body;
                    });
                })
                .then(function (result) {
                    el('stagingImportStatus').textContent =
                        result.status + ' - 신규 ' + result.insertedCount + '건, 수정 ' + result.updatedCount + '건';
                    loadSyncLogs();
                    loadStores();
                    loadCurrencyTypes();
                })
                .catch(function (err) {
                    el('stagingImportStatus').textContent = err.message;
                });
        });

        el('storeForm').addEventListener('submit', function (event) {
            event.preventDefault();
            var id = el('storeId').value;
            var payload = {
                name: el('formName').value,
                currencyTypeId: Number(el('formCurrencyTypeId').value),
                roadAddress: el('formRoadAddress').value,
                jibunAddress: el('formJibunAddress').value,
                phone: el('formPhone').value,
                category: el('formCategory').value,
                bizStatus: el('formBizStatus').value
            };

            var url = id ? '/admin/api/stores/' + id : '/admin/api/stores';
            var method = id ? 'PUT' : 'POST';

            fetch(url, { method: method, headers: authHeaders(), body: JSON.stringify(payload) })
                .then(function (res) {
                    if (!res.ok) throw new Error('저장 실패');
                    return res.json();
                })
                .then(function () {
                    resetForm();
                    loadStores();
                })
                .catch(function (err) {
                    alert(err.message);
                });
        });
    });
})();
