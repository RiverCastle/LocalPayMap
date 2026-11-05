package com.localpaymap.dto;

import com.localpaymap.domain.FavoriteGroup;
import java.util.List;

/** storeIds는 지도에서 마커 색칠과 "이 그룹에 담겼는지" 표시에 쓰인다. */
public record FavoriteGroupResponse(Long id, String name, String color, List<Long> storeIds) {

    public static FavoriteGroupResponse of(FavoriteGroup group, List<Long> storeIds) {
        return new FavoriteGroupResponse(group.getId(), group.getName(), group.getColor(), storeIds);
    }
}
