package com.localpaymap.controller.api;

import com.localpaymap.auth.AuthTokenService;
import com.localpaymap.auth.TokenCookies;
import com.localpaymap.auth.TokenPair;
import com.localpaymap.domain.AppUser;
import com.localpaymap.dto.MeResponse;
import com.localpaymap.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthApiController {

    private final AuthTokenService tokenService;
    private final TokenCookies tokenCookies;
    private final AppUserRepository appUserRepository;

    public AuthApiController(
            AuthTokenService tokenService, TokenCookies tokenCookies, AppUserRepository appUserRepository) {
        this.tokenService = tokenService;
        this.tokenCookies = tokenCookies;
        this.appUserRepository = appUserRepository;
    }

    /** 비로그인도 200으로 응답해 지도 첫 화면에서 불필요한 401 오류가 쌓이지 않게 한다. */
    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            Optional<AppUser> user = appUserRepository.findById(Long.valueOf(jwt.getToken().getSubject()));
            if (user.isPresent()) {
                return MeResponse.of(user.get());
            }
        }
        return MeResponse.anonymous();
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        Optional<TokenPair> pair = tokenService.rotate(TokenCookies.read(request, TokenCookies.REFRESH));
        if (pair.isEmpty()) {
            tokenCookies.clear(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        tokenCookies.write(response, pair.get());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        tokenService.revoke(TokenCookies.read(request, TokenCookies.REFRESH));
        tokenCookies.clear(response);
        return ResponseEntity.noContent().build();
    }
}
