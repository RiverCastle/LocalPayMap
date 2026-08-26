package com.localpaymap.dto;

import com.localpaymap.repository.StoreClusterProjection;

public record ClusterResponse(double lat, double lng, long count) {

    public static ClusterResponse from(StoreClusterProjection projection) {
        return new ClusterResponse(projection.getAvgLat(), projection.getAvgLng(), projection.getCnt());
    }
}
