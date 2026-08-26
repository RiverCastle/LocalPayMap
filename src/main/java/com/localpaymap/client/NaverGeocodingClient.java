package com.localpaymap.client;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 네이버 클라우드 플랫폼 Geocoding API(주소 검색)를 이용해 도로명주소를 위경도로 변환한다.
 * 콘솔에서 발급받은 Client ID/Secret이 필요하며, base-url은 NCP 콘솔의 최신 안내를 따라 조정할 것.
 */
@Component
public class NaverGeocodingClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public NaverGeocodingClient(
            RestClient.Builder restClientBuilder,
            @Value("${naver.geocode-base-url:https://maps.apigw.ntruss.com/map-geocode/v2}") String baseUrl,
            @Value("${naver.client-id:}") String clientId,
            @Value("${naver.client-secret:}") String clientSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Optional<GeoPoint> geocode(String address) {
        NaverGeocodeResponse response = restClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path("/geocode").queryParam("query", address).build())
                .header("X-NCP-APIGW-API-KEY-ID", clientId)
                .header("X-NCP-APIGW-API-KEY", clientSecret)
                .retrieve()
                .body(NaverGeocodeResponse.class);

        if (response == null) {
            return Optional.empty();
        }
        List<NaverGeocodeResponse.Address> addresses = response.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            return Optional.empty();
        }
        NaverGeocodeResponse.Address address0 = addresses.get(0);
        return Optional.of(new GeoPoint(Double.parseDouble(address0.getY()), Double.parseDouble(address0.getX())));
    }

    public record GeoPoint(double lat, double lng) {}
}
