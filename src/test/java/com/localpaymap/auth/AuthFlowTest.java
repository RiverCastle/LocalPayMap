package com.localpaymap.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.localpaymap.domain.AppUser;
import com.localpaymap.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuthTokenService tokenService;
    @Autowired AppUserRepository appUserRepository;

    private AppUser newUser(String providerId) {
        return appUserRepository.save(new AppUser("kakao", providerId, "테스터", null));
    }

    @Test
    void refreshTokenRotatesAndReuseRevokesEverything() {
        TokenPair first = tokenService.issue(newUser("rot-1"));

        TokenPair second = tokenService.rotate(first.refreshToken()).orElseThrow();
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

        // 이미 사용된 토큰 재사용 -> 실패 + 새로 발급된 토큰까지 무효화
        assertThat(tokenService.rotate(first.refreshToken())).isEmpty();
        assertThat(tokenService.rotate(second.refreshToken())).isEmpty();
    }

    @Test
    void revokedRefreshTokenCannotBeUsed() {
        TokenPair pair = tokenService.issue(newUser("rev-1"));
        tokenService.revoke(pair.refreshToken());
        assertThat(tokenService.rotate(pair.refreshToken())).isEmpty();
    }

    @Test
    void meIsPublicAndReportsAnonymous() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void meReturnsProfileWithAccessCookie() throws Exception {
        AppUser user = newUser("me-1");
        TokenPair pair = tokenService.issue(user);
        mockMvc.perform(get("/api/me").cookie(new Cookie(TokenCookies.ACCESS, pair.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.nickname").value("테스터"));
    }

    @Test
    void personalizedApiRequiresLoginButPublicApiDoesNot() throws Exception {
        mockMvc.perform(get("/api/me/example")).andExpect(status().isUnauthorized());

        TokenPair pair = tokenService.issue(newUser("acl-1"));
        mockMvc.perform(// 인증은 통과했지만 해당 API가 아직 없어 404 (401/403이 아님)
                get("/api/me/example").cookie(new Cookie(TokenCookies.ACCESS, pair.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void expiredOrGarbageCookieDoesNotBreakPublicApi() throws Exception {
        mockMvc.perform(get("/api/categories").cookie(new Cookie(TokenCookies.ACCESS, "garbage.token.value")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me").cookie(new Cookie(TokenCookies.ACCESS, "garbage.token.value")))
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void refreshEndpointRequiresCsrfAndValidCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("XSRF-TOKEN", "t")).header("X-XSRF-TOKEN", "t")).andExpect(status().isUnauthorized());

        TokenPair pair = tokenService.issue(newUser("ref-1"));
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("XSRF-TOKEN", "t")).header("X-XSRF-TOKEN", "t")
                        .cookie(new Cookie(TokenCookies.REFRESH, pair.refreshToken())))
                .andExpect(status().isNoContent());
    }

    @Test
    void cookieAuthenticatedWritesRequireCsrfToken() throws Exception {
        TokenPair pair = tokenService.issue(newUser("csrf-1"));
        Cookie access = new Cookie(TokenCookies.ACCESS, pair.accessToken());
        mockMvc.perform(post("/api/auth/logout").cookie(access)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(access, new Cookie("XSRF-TOKEN", "t"))
                        .header("X-XSRF-TOKEN", "t"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminAreaStillRequiresAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/stores")).andExpect(status().is3xxRedirection());
    }
}
