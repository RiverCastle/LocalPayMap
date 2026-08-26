package com.localpaymap.dto;

import java.util.List;

/** clustered=true면 stores는 비고 clusters(격자 중심좌표+개수)만 채워진다. */
public record StoreSearchResponse(boolean clustered, List<StoreResponse> stores, List<ClusterResponse> clusters) {

    public static StoreSearchResponse individual(List<StoreResponse> stores) {
        return new StoreSearchResponse(false, stores, List.of());
    }

    public static StoreSearchResponse clustered(List<ClusterResponse> clusters) {
        return new StoreSearchResponse(true, List.of(), clusters);
    }
}
