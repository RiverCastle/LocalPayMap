package com.localpaymap.auth;

import com.localpaymap.domain.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** 소셜 인증은 일회성으로만 쓰고, 서버에는 인증 상태를 남기지 않은 채 JWT 쿠키를 발급한다. */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialUserService socialUserService;
    private final AuthTokenService tokenService;
    private final TokenCookies tokenCookies;

    public OAuth2LoginSuccessHandler(
            SocialUserService socialUserService, AuthTokenService tokenService, TokenCookies tokenCookies) {
        this.socialUserService = socialUserService;
        this.tokenService = tokenService;
        this.tokenCookies = tokenCookies;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AppUser user = socialUserService.loginOrSignUp(token.getAuthorizedClientRegistrationId(), token.getPrincipal());
        tokenCookies.write(response, tokenService.issue(user));
        response.sendRedirect("/");
    }
}
