(function () {
    var map;
    var markers = [];
    var CLUSTER_ZOOM_STEP = 2;
    var NEARBY_ZOOM = 16;

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

    var NAVER_POPUP_NAME = 'naverPlacePopup';

    /**
     * 네이버 지도 장소 ID(place/12345...)는 우리 DB에 없고 공식 API로도 얻을 수 없어서,
     * 동일 상호 오검색을 막기 위해 "상호 + 주소"로 검색 URL을 만든다. (결과가 한 곳이면 네이버가 장소 상세를 바로 연다)
     * 주소는 도로명주소를 우선 쓰고, 없으면 지번주소에서 "번지"와 층/호 정보를 떼어 쓴다.
     */
    function naverSearchQuery(store) {
        var address = store.roadAddress;
        if (!address && store.jibunAddress) {
            address = store.jibunAddress
                .replace(/번지/g, '')
                .replace(/\s+(지하\s*)?\d+층.*$/, '')
                .trim();
        }
        return (store.name + ' ' + (address || '')).trim();
    }

    function naverPlaceUrl(store) {
        return 'https://map.naver.com/p/search/' + encodeURIComponent(naverSearchQuery(store));
    }

    /** 같은 이름의 창을 재사용하므로 마커를 연속 클릭해도 팝업이 계속 늘어나지 않는다. */
    function openNaverPlacePopup(store) {
        var width = 520;
        var height = Math.min(window.screen.availHeight - 80, 860);
        var left = Math.max(0, window.screenX + window.outerWidth - width - 20);
        var top = Math.max(0, window.screenY + 40);
        var popup = window.open(
            naverPlaceUrl(store),
            NAVER_POPUP_NAME,
            'popup=yes,width=' + width + ',height=' + height + ',left=' + left + ',top=' + top + ',resizable=yes,scrollbars=yes'
        );
        if (popup) {
            popup.focus();
        }
        return popup;
    }

    function showDetail(store) {
        var link = el('detailNaverLink');
        link.href = naverPlaceUrl(store);
        link.onclick = function (event) {
            // 팝업 차단 등으로 열리지 않으면 기본 동작(새 탭)으로 폴백한다.
            if (openNaverPlacePopup(store)) event.preventDefault();
        };
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

    var PIN_WIDTH = 22;
    var PIN_HEIGHT = 30;
    // 이 줌 이상에서만 상호를 항상 표시한다. (그보다 멀리서는 라벨이 서로 겹쳐 읽을 수 없으므로 마우스를 올릴 때만 표시)
    var LABEL_MIN_ZOOM = 16;

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    var GROUP_PIN_WIDTH = 30;
    var GROUP_PIN_HEIGHT = 40;

    /** 단일 가맹점 마커: 파란 핀 + 상호 라벨 */
    function storeMarkerHtml(store, showLabel) {
        return (
            '<div class="store-marker' +
            (showLabel ? ' labeled' : '') +
            '">' +
            '<svg class="store-pin" width="' + PIN_WIDTH + '" height="' + PIN_HEIGHT + '" viewBox="0 0 22 30">' +
            '<path d="M11 0C4.9 0 0 4.9 0 11c0 8.2 11 19 11 19s11-10.8 11-19C22 4.9 17.1 0 11 0z" fill="#2f6fed"/>' +
            '<circle cx="11" cy="11" r="4.5" fill="#fff"/></svg>' +
            '<span class="store-label">' + escapeHtml(store.name) + '</span>' +
            '</div>'
        );
    }

    /** 같은 좌표(같은 건물)에 여러 가맹점이 있을 때의 마커: 주황 핀 + 점포 수 */
    function groupMarkerHtml(group, showLabel) {
        return (
            '<div class="store-marker group-marker' +
            (showLabel ? ' labeled' : '') +
            '" style="width:' + GROUP_PIN_WIDTH + 'px;height:' + GROUP_PIN_HEIGHT + 'px">' +
            '<svg class="store-pin" width="' + GROUP_PIN_WIDTH + '" height="' + GROUP_PIN_HEIGHT + '" viewBox="0 0 30 40">' +
            '<path d="M15 0C6.7 0 0 6.7 0 15c0 11 15 25 15 25s15-14 15-25C30 6.7 23.3 0 15 0z" fill="#e8590c"/>' +
            '<circle cx="15" cy="15" r="10.5" fill="#fff"/>' +
            '<text x="15" y="19" text-anchor="middle" font-size="' + (group.stores.length >= 100 ? 10 : 12) +
            '" font-weight="700" fill="#e8590c">' + group.stores.length + '</text></svg>' +
            '<span class="store-label">' + group.stores.length + '곳</span>' +
            '</div>'
        );
    }

    /** 위경도가 완전히 같은 가맹점을 하나로 묶는다. (한 건물에 입점한 점포들은 건물 대표 좌표를 공유한다) */
    function groupByLocation(stores) {
        var byKey = {};
        var groups = [];
        stores.forEach(function (store) {
            var key = store.lat + ',' + store.lng;
            var group = byKey[key];
            if (!group) {
                group = { key: key, lat: store.lat, lng: store.lng, stores: [] };
                byKey[key] = group;
                groups.push(group);
            }
            group.stores.push(store);
        });
        return groups;
    }

    var currentGroups = [];
    var selectedGroupKey = null;

    function appendStoreListItem(listEl, store, onClick) {
        var item = document.createElement('div');
        item.className = 'store-list-item';
        item.textContent = store.name + ' (' + (store.roadAddress || store.jibunAddress || '') + ')';
        item.addEventListener('click', onClick);
        listEl.appendChild(item);
    }

    /** 오른쪽 패널: 선택된 건물이 있으면 그 건물의 점포만(이름 검색 가능), 없으면 화면 안의 전체 가맹점 */
    function renderStoreList() {
        var listEl = el('storeList');
        listEl.innerHTML = '';
        var selected = currentGroups.filter(function (g) {
            return g.key === selectedGroupKey;
        })[0];

        if (!selected) {
            selectedGroupKey = null;
            currentGroups.forEach(function (group) {
                group.stores.forEach(function (store) {
                    appendStoreListItem(listEl, store, function () {
                        map.panTo(new naver.maps.LatLng(group.lat, group.lng));
                        showDetail(store);
                    });
                });
            });
            return;
        }

        var header = document.createElement('div');
        header.className = 'group-header';
        var title = document.createElement('strong');
        title.textContent = '이 위치의 가맹점 ' + selected.stores.length + '곳';
        var back = document.createElement('button');
        back.type = 'button';
        back.textContent = '전체 목록';
        back.addEventListener('click', function () {
            selectedGroupKey = null;
            renderStoreList();
        });
        header.appendChild(title);
        header.appendChild(back);
        listEl.appendChild(header);

        var filter = document.createElement('input');
        filter.type = 'text';
        filter.className = 'group-filter';
        filter.placeholder = '이 건물 안에서 상호 검색';
        listEl.appendChild(filter);

        var itemsEl = document.createElement('div');
        listEl.appendChild(itemsEl);

        function renderItems() {
            var query = filter.value.trim().toLowerCase();
            itemsEl.innerHTML = '';
            selected.stores.forEach(function (store) {
                if (query && store.name.toLowerCase().indexOf(query) < 0) return;
                appendStoreListItem(itemsEl, store, function () {
                    showDetail(store);
                    openNaverPlacePopup(store);
                });
            });
        }
        filter.addEventListener('input', renderItems);
        renderItems();
    }

    function renderStores(stores) {
        clearMarkers();
        currentGroups = groupByLocation(stores);
        var showLabels = map.getZoom() >= LABEL_MIN_ZOOM;

        currentGroups.forEach(function (group) {
            var position = new naver.maps.LatLng(group.lat, group.lng);
            var multiple = group.stores.length > 1;
            var marker = new naver.maps.Marker({
                position: position,
                map: map,
                // 건물 마커가 단일 마커 위에 오도록 한다.
                zIndex: multiple ? 20 : 10,
                icon: {
                    content: multiple
                        ? groupMarkerHtml(group, showLabels)
                        : storeMarkerHtml(group.stores[0], showLabels),
                    anchor: multiple
                        ? new naver.maps.Point(GROUP_PIN_WIDTH / 2, GROUP_PIN_HEIGHT)
                        : new naver.maps.Point(PIN_WIDTH / 2, PIN_HEIGHT)
                }
            });
            naver.maps.Event.addListener(marker, 'click', function () {
                if (multiple) {
                    // 점포가 여러 곳이면 팝업 대신 목록에서 고르게 한다.
                    selectedGroupKey = group.key;
                    el('storeDetail').classList.add('hidden');
                    renderStoreList();
                } else {
                    showDetail(group.stores[0]);
                    openNaverPlacePopup(group.stores[0]);
                }
            });
            markers.push(marker);
        });

        renderStoreList();
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

    var myMarker = null;

    /** 현재 위치를 파란 점으로 표시한다. (가맹점 마커와 별개라 조회 시 지워지지 않는다) */
    function showMyLocationMarker(position) {
        if (myMarker) {
            myMarker.setPosition(position);
            return;
        }
        myMarker = new naver.maps.Marker({
            position: position,
            map: map,
            zIndex: 100,
            icon: {
                content: '<div class="my-location-marker"></div>',
                anchor: new naver.maps.Point(9, 9)
            }
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
            navigator.geolocation.getCurrentPosition(
                function (position) {
                    var loc = new naver.maps.LatLng(position.coords.latitude, position.coords.longitude);
                    // 이동하면 map의 idle 이벤트로 해당 위치의 가맹점이 자동 조회된다.
                    showMyLocationMarker(loc);
                    map.setCenter(loc);
                    map.setZoom(NEARBY_ZOOM);
                },
                function (err) {
                    alert(err.code === 1 ? '위치 권한이 거부되었습니다. 브라우저 설정에서 허용해 주세요.' : '현재 위치를 가져오지 못했습니다.');
                },
                { enableHighAccuracy: true, timeout: 10000 }
            );
        });

        bindCategoryInput();
        loadCurrencyTypes();
        loadCategories();
        fetchStores();
    }

    document.addEventListener('DOMContentLoaded', initMap);
})();
