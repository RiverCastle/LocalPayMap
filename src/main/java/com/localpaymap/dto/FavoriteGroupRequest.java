package com.localpaymap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** color를 생략하면 생성 시에는 서버가 기본 팔레트에서 고르고, 수정 시에는 기존 색을 유지한다. */
public record FavoriteGroupRequest(
        @NotBlank(message = "그룹 이름을 입력해 주세요.") @Size(max = 50, message = "그룹 이름은 50자 이하로 입력해 주세요.") String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "색상은 #RRGGBB 형식이어야 합니다.") String color) {}
