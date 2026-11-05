package com.localpaymap.auth;

import com.localpaymap.domain.AppUser;
import com.localpaymap.repository.AppUserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소셜 제공자별 응답 형식을 AppUser로 매핑하고, 최초 로그인이면 가입시킨다. 이메일은 수집하지 않는다. */
@Service
public class SocialUserService {

    private final AppUserRepository appUserRepository;

    public SocialUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser loginOrSignUp(String registrationId, OAuth2User oauthUser) {
        SocialProfile profile = extract(registrationId, oauthUser.getAttributes());
        AppUser user = appUserRepository
                .findByProviderAndProviderId(registrationId, profile.providerId())
                .orElseGet(() -> new AppUser(
                        registrationId, profile.providerId(), profile.nickname(), profile.profileImage()));
        user.setNickname(profile.nickname());
        user.setProfileImage(profile.profileImage());
        user.setLastLoginAt(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    @SuppressWarnings("unchecked")
    static SocialProfile extract(String registrationId, Map<String, Object> attrs) {
        if ("kakao".equals(registrationId)) {
            String id = String.valueOf(attrs.get("id"));
            Map<String, Object> account = (Map<String, Object>) attrs.getOrDefault("kakao_account", Map.of());
            Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
            String nickname = (String) profile.get("nickname");
            if (nickname == null) {
                Map<String, Object> legacy = (Map<String, Object>) attrs.getOrDefault("properties", Map.of());
                nickname = (String) legacy.get("nickname");
            }
            return new SocialProfile(
                    id, nickname == null ? "사용자" : nickname, (String) profile.get("profile_image_url"));
        }
        throw new IllegalArgumentException("지원하지 않는 로그인 제공자입니다: " + registrationId);
    }

    record SocialProfile(String providerId, String nickname, String profileImage) {}
}
