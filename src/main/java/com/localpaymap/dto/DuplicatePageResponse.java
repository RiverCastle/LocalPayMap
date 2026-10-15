package com.localpaymap.dto;

import java.util.List;

public record DuplicatePageResponse(long totalGroups, int page, int size, List<DuplicateGroupResponse> groups) {}
