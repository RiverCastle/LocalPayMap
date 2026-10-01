(function () {
    var map;
    var markers = [];
    var CLUSTER_ZOOM_STEP = 2;

    function el(id) {
        return document.getElementById(id);
    }

    function loadCurrencyTypes() {
        fetch('/api/currency-types')
            .then(function (res) {
                return res.json();
            })
            .then(function (types) {
                var select = el('currencyTypeSelect');
                types.forEach(function (type) {
                    var option = document.createElement('option');
                    option.value = type.id;
                    option.textContent = type.regionName + ' - ' + type.currencyName;
                    select.appendChild(option);
                });
            })
            .catch(function (err) {
                console.error('지역화폐 종류 조회 실패', err);
            });
    }

    // 업종 필터 상태: 그룹 칩(여러 개 선택 가능) + 자동완성으로 고른 세부 업종
    var selectedGroups = [];
    var selectedItems = []; // { value, label }
    var categoryData = { groups: [], items: [] };
    var activeSuggestion = -1;

    function toggle(list, predicate, entry) {
        var idx = list.findIndex(predicate);
        if (idx >= 0) list.splice(idx, 1);
        else list.push(entry);
    }

    function renderChips() {
        var box = el('categoryChips');
        box.innerHTML = '';

        function addChip(label, count, selected, extraClass, onClick) {
            var chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'chip' + (selected ? ' selected' : '') + (extraClass ? ' ' + extraClass : '');
            chip.textContent = label;
            if (count != null) {
                var span = document.createElement('span');
                span.className = 'chip-count';
                span.textContent = count.toLocaleString();
                chip.appendChild(span);
            }
            chip.addEventListener('click', onClick);
            box.appendChild(chip);
        }

        if (categoryData.groups.length === 0) return;

        var anySelected = selectedGroups.length > 0 || selectedItems.length > 0;
        addChip('전체', null, !anySelected, '', function () {
            selectedGroups = [];
            selectedItems = [];
            renderChips();
            fetchStores();
        });
        categoryData.groups.forEach(function (group) {
            addChip(group.name, group.count, selectedGroups.indexOf(group.name) >= 0, '', function () {
                toggle(selectedGroups, function (g) { return g === group.name; }, group.name);
                renderChips();
                fetchStores();
            });
        });
        selectedItems.forEach(function (item) {
            addChip(item.label, null, true, 'chip-item', function () {
                toggle(selectedItems, function (i) { return i.value === item.value; }, item);
                renderChips();
                fetchStores();
            });
        });
    }

    function loadCategories() {
        fetch('/api/categories')
            .then(function (res) {
                return res.json();
            })
            .then(function (data) {
                categoryData = data;
                renderChips();
            })
            .catch(function (err) {
                console.error('업종 목록 조회 실패', err);
            });
    }

    function hideSuggestions() {
        el('categorySuggestions').classList.add('hidden');
        activeSuggestion = -1;
    }

    function pickSuggestion(item) {
        if (!selectedItems.some(function (i) { return i.value === item.value; })) {
            selectedItems.push({ value: item.value, label: item.label });
        }
        el('categoryInput').value = '';
        hideSuggestions();
        renderChips();
        fetchStores();
    }

    function renderSuggestions() {
        var query = el('categoryInput').value.trim().toLowerCase();
        var list = el('categorySuggestions');
        list.innerHTML = '';
        activeSuggestion = -1;
        if (!query) {
            hideSuggestions();
            return;
        }
        var matches = categoryData.items
            .filter(function (item) {
                return item.label.toLowerCase().indexOf(query) >= 0 || item.group.indexOf(query) >= 0;
            })
            .slice(0, 8);
        if (matches.length === 0) {
            hideSuggestions();
            return;
        }
        matches.forEach(function (item) {
            var li = document.createElement('li');
            var name = document.createElement('span');
            name.textContent = item.label;
            var meta = document.createElement('span');
            meta.className = 'suggestion-meta';
            meta.textContent = item.group + ' · ' + item.count.toLocaleString() + '곳';
            li.appendChild(name);
            li.appendChild(meta);
            // blur보다 먼저 선택되도록 mousedown 사용
            li.addEventListener('mousedown', function (event) {
                event.preventDefault();
                pickSuggestion(item);
            });
            list.appendChild(li);
        });
        list.classList.remove('hidden');
    }

    function moveSuggestion(delta) {
        var items = el('categorySuggestions').children;
        if (!items.length) return;
        if (activeSuggestion >= 0) items[activeSuggestion].classList.remove('active');
        activeSuggestion = (activeSuggestion + delta + items.length) % items.length;
        items[activeSuggestion].classList.add('active');
    }

    function bindCategoryInput() {
        var input = el('categoryInput');
        input.addEventListener('input', renderSuggestions);
        input.addEventListener('blur', hideSuggestions);
        input.addEventListener('keydown', function (event) {
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                moveSuggestion(1);
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                moveSuggestion(-1);
            } else if (event.key === 'Enter') {
                var items = el('categorySuggestions').children;
                if (items.length) {
                    event.preventDefault();
                    var target = items[activeSuggestion >= 0 ? activeSuggestion : 0];
                    target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
                }
            } else if (event.key === 'Escape') {
                hideSuggestions();
            }
        });
    }

    function clearMarkers() {
        markers.forEach(function (marker) {
            marker.setMap(null);
        });
        markers = [];
    }

    function showDetail(store) {
        el('detailName').textContent = store.name;
        el('detailAddress').textContent = store.roadAddress || store.jibunAddress || '';
        el('detailPhone').textContent = store.phone ? '전화: ' + store.phone : '';
        el('detailCategory').textContent = store.category ? '업종: ' + store.category : '';
        el('detailCurrency').textContent = store.currencyName ? '지역화폐: ' + store.currencyName : '';
        el('storeDetail').classList.remove('hidden');
    }

    function clusterSizeClass(count) {
        if (count >= 1000) return 'cluster-xl';
        if (count >= 200) return 'cluster-lg';
        if (count >= 50) return 'cluster-md';
        return 'cluster-sm';
    }

    function renderClusters(clusters) {
        clearMarkers();
        var listEl = el('storeList');
        listEl.innerHTML = '';
        var notice = document.createElement('div');
        notice.className = 'store-list-notice';
        notice.textContent = '가맹점이 너무 많아 묶어서 표시 중입니다. 지도를 확대하면 개별 가맹점이 보입니다.';
        listEl.appendChild(notice);

        clusters.forEach(function (cluster) {
            var position = new naver.maps.LatLng(cluster.lat, cluster.lng);
            var marker = new naver.maps.Marker({
                position: position,
                map: map,
                icon: {
                    content:
                        '<div class="cluster-marker ' +
                        clusterSizeClass(cluster.count) +
                        '">' +
                        cluster.count.toLocaleString() +
                        '</div>',
                    anchor: new naver.maps.Point(20, 20)
                }
            });
            naver.maps.Event.addListener(marker, 'click', function () {
                map.morph(position, Math.min(map.getZoom() + CLUSTER_ZOOM_STEP, 21));
            });
            markers.push(marker);
        });
    }

    function renderStores(stores) {
        clearMarkers();
        var listEl = el('storeList');
        listEl.innerHTML = '';

        stores.forEach(function (store) {
            var position = new naver.maps.LatLng(store.lat, store.lng);
            var marker = new naver.maps.Marker({ position: position, map: map });
            naver.maps.Event.addListener(marker, 'click', function () {
                showDetail(store);
            });
            markers.push(marker);

            var item = document.createElement('div');
            item.className = 'store-list-item';
            item.textContent = store.name + ' (' + (store.roadAddress || '') + ')';
            item.addEventListener('click', function () {
                map.panTo(position);
                showDetail(store);
            });
            listEl.appendChild(item);
        });
    }

    function fetchStores() {
        var bounds = map.getBounds();
        var sw = bounds.getSW();
        var ne = bounds.getNE();

        var params = new URLSearchParams({
            swLat: sw.lat(),
            swLng: sw.lng(),
            neLat: ne.lat(),
            neLng: ne.lng(),
            zoom: map.getZoom()
        });

        var currencyTypeId = el('currencyTypeSelect').value;
        var keyword = el('keywordInput').value.trim();
        if (currencyTypeId) params.set('currencyTypeId', currencyTypeId);
        if (keyword) params.set('keyword', keyword);
        // 업종값에 쉼표가 들어 있어 쉼표 구분 대신 반복 파라미터로 보낸다.
        selectedGroups.forEach(function (group) {
            params.append('categoryGroups', group);
        });
        selectedItems.forEach(function (item) {
            params.append('categories', item.value);
        });

        fetch('/api/stores?' + params.toString())
            .then(function (res) {
                return res.json();
            })
            .then(function (result) {
                if (result.clustered) {
                    renderClusters(result.clusters);
                } else {
                    renderStores(result.stores);
                }
            })
            .catch(function (err) {
                console.error('가맹점 조회 실패', err);
            });
    }

    function initMap() {
        var container = document.getElementById('map');
        var defaultCenter = new naver.maps.LatLng(37.5665, 126.978); // 서울시청 기본 위치
        map = new naver.maps.Map(container, { center: defaultCenter, zoom: 15 });

        naver.maps.Event.addListener(map, 'idle', fetchStores);

        el('searchButton').addEventListener('click', fetchStores);
        el('closeDetailButton').addEventListener('click', function () {
            el('storeDetail').classList.add('hidden');
        });
        el('locateButton').addEventListener('click', function () {
            if (!navigator.geolocation) return;
            navigator.geolocation.getCurrentPosition(function (position) {
                var loc = new naver.maps.LatLng(position.coords.latitude, position.coords.longitude);
                map.setCenter(loc);
            });
        });

        bindCategoryInput();
        loadCurrencyTypes();
        loadCategories();
        fetchStores();
    }

    document.addEventListener('DOMContentLoaded', initMap);
})();
