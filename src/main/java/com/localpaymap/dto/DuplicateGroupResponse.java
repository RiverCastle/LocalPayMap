package com.localpaymap.dto;

import java.util.List;

public record DuplicateGroupResponse(
        String name, String roadAddress, Long currencyTypeId, List<StoreResponse> stores) {}
