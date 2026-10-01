package com.localpaymap.service;

import com.localpaymap.client.NaverGeocodingClient;
import com.localpaymap.domain.CurrencyType;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import com.localpaymap.dto.ClusterResponse;
import com.localpaymap.dto.StoreResponse;
import com.localpaymap.dto.StoreSearchResponse;
import com.localpaymap.dto.StoreUpsertRequest;
import com.localpaymap.repository.CurrencyTypeRepository;
import com.localpaymap.repository.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StoreService {

    /** 이 개수를 넘으면 개별 마커 대신 격자 클러스터로 묶어서 내려준다 (브라우저 렌더링 부하 방지). */
    private static final long CLUSTER_THRESHOLD = 300;

    private final StoreRepository storeRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final NaverGeocodingClient naverGeocodingClient;
    private final CategoryService categoryService;

    public StoreService(
            StoreRepository storeRepository,
            CurrencyTypeRepository currencyTypeRepository,
            NaverGeocodingClient naverGeocodingClient,
            CategoryService categoryService) {
        this.storeRepository = storeRepository;
        this.currencyTypeRepository = currencyTypeRepository;
        this.naverGeocodingClient = naverGeocodingClient;
        this.categoryService = categoryService;
    }

    public StoreSearchResponse searchInBounds(
            double swLat,
            double swLng,
            double neLat,
            double neLng,
            Long currencyTypeId,
            List<String> categoryGroups,
            List<String> categories,
            String keyword,
            int zoom) {
        List<String> resolved = categoryService.resolve(categoryGroups, categories);
        int categoryCount = resolved.size();
        // 업종 조건이 없을 때도 IN 절에는 값이 하나 필요해서 빈 문자열을 넣는다 (categoryCount=0이면 조건이 무시됨).
        List<String> categoryParam = resolved.isEmpty() ? List.of("") : resolved;

        long count = storeRepository.countInBounds(
                swLat, swLng, neLat, neLng, currencyTypeId, categoryCount, categoryParam, keyword);

        if (count > CLUSTER_THRESHOLD) {
            double cellSize = gridCellSize(zoom);
            List<ClusterResponse> clusters = storeRepository
                    .clusterInBounds(
                            swLat, swLng, neLat, neLng, currencyTypeId, categoryCount, categoryParam, keyword, cellSize)
                    .stream()
                    .map(ClusterResponse::from)
                    .toList();
            return StoreSearchResponse.clustered(clusters);
        }

        List<StoreResponse> stores = storeRepository
                .searchInBounds(swLat, swLng, neLat, neLng, currencyTypeId, categoryCount, categoryParam, keyword)
                .stream()
                .map(StoreResponse::from)
                .toList();
        return StoreSearchResponse.individual(stores);
    }

    /** 격자 한 칸이 화면에서 대략 이 픽셀 크기가 되도록 한다. */
    private static final double CLUSTER_CELL_PIXELS = 80;

    /** 슬리피맵 타일과 비슷하게, 확대할수록(zoom↑) 격자 한 칸이 좁아지도록 근사한다. */
    private double gridCellSize(int zoom) {
        int safeZoom = Math.max(0, Math.min(zoom, 21));
        double degreesPerPixel = 360.0 / (256.0 * Math.pow(2, safeZoom));
        return degreesPerPixel * CLUSTER_CELL_PIXELS;
    }

    public List<StoreResponse> listAll(org.springframework.data.domain.Pageable pageable) {
        return storeRepository.findAll(pageable).stream().map(StoreResponse::from).toList();
    }

    public StoreResponse getById(Long id) {
        return storeRepository
                .findById(id)
                .map(StoreResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가맹점입니다: " + id));
    }

    @Transactional
    public StoreResponse createManual(StoreUpsertRequest request) {
        CurrencyType currencyType = getCurrencyTypeOrThrow(request.currencyTypeId());
        NaverGeocodingClient.GeoPoint point = geocodeOrThrow(request.roadAddress());

        Store store = new Store(
                request.name(),
                request.roadAddress(),
                request.jibunAddress(),
                point.lat(),
                point.lng(),
                request.phone(),
                request.category(),
                currencyType,
                request.bizStatus(),
                StoreSource.MANUAL,
                null);
        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public StoreResponse update(Long id, StoreUpsertRequest request) {
        Store store = storeRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가맹점입니다: " + id));
        CurrencyType currencyType = getCurrencyTypeOrThrow(request.currencyTypeId());
        NaverGeocodingClient.GeoPoint point = geocodeOrThrow(request.roadAddress());

        Store updated = new Store(
                request.name(),
                request.roadAddress(),
                request.jibunAddress(),
                point.lat(),
                point.lng(),
                request.phone(),
                request.category(),
                currencyType,
                request.bizStatus(),
                store.getSource(),
                store.getExternalId());
        store.updateFrom(updated);
        return StoreResponse.from(store);
    }

    @Transactional
    public void delete(Long id) {
        storeRepository.deleteById(id);
    }

    private CurrencyType getCurrencyTypeOrThrow(Long currencyTypeId) {
        return currencyTypeRepository
                .findById(currencyTypeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역화폐 종류입니다: " + currencyTypeId));
    }

    private NaverGeocodingClient.GeoPoint geocodeOrThrow(String roadAddress) {
        return naverGeocodingClient
                .geocode(roadAddress)
                .orElseThrow(() -> new IllegalArgumentException("주소를 좌표로 변환할 수 없습니다: " + roadAddress));
    }
}
