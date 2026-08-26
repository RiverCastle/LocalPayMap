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
        var category = el('categoryInput').value.trim();
        var keyword = el('keywordInput').value.trim();
        if (currencyTypeId) params.set('currencyTypeId', currencyTypeId);
        if (category) params.set('category', category);
        if (keyword) params.set('keyword', keyword);

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

        loadCurrencyTypes();
        fetchStores();
    }

    document.addEventListener('DOMContentLoaded', initMap);
})();
