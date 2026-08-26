package com.localpaymap.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 공공데이터포털 "전국지역화폐가맹점표준데이터" Open API 클라이언트.
 * 활용신청 승인 후 발급받은 서비스키를 open-data.service-key 로 설정해야 동작한다.
 */
@Component
public class OpenDataApiClient {

    private final RestClient restClient;
    private final String serviceKey;

    public OpenDataApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${open-data.base-url}") String baseUrl,
            @Value("${open-data.service-key:}") String serviceKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.serviceKey = serviceKey;
    }

    public List<OpenDataStoreItem> fetchPage(int pageNo, int numOfRows) {
        OpenDataResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("type", "json")
                        .build())
                .retrieve()
                .body(OpenDataResponse.class);

        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
            return List.of();
        }
        List<OpenDataStoreItem> items = response.getResponse().getBody().getItems();
        return items != null ? items : List.of();
    }

    public int fetchTotalCount(int numOfRows) {
        OpenDataResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("type", "json")
                        .build())
                .retrieve()
                .body(OpenDataResponse.class);
        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
            return 0;
        }
        return response.getResponse().getBody().getTotalCount();
    }
}
