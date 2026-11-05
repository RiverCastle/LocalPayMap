package com.localpaymap.dto;

import com.localpaymap.domain.AppUser;

public record MeResponse(boolean authenticated, Long id, String nickname, String profileImage) {

    public static MeResponse of(AppUser user) {
        return new MeResponse(true, user.getId(), user.getNickname(), user.getProfileImage());
    }

    public static MeResponse anonymous() {
        return new MeResponse(false, null, null, null);
    }
}
