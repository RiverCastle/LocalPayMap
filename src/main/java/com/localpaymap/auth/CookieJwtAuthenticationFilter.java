package com.localpaymap.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * access 토큰 쿠키로 인증한다. Authorization 헤더가 있으면 resource server 쪽에 맡긴다.
 *
 * <p>쿠키 인증을 resource server의 BearerTokenResolver로 처리하면 Spring이 그 요청의 CSRF 검사를 자동으로 제외하므로,
 * 쿠키는 별도 필터로 인증해 CSRF 방어가 유지되게 한다. 쿠키 토큰이 만료/위조된 경우에는 401을 내지 않고 익명으로 취급한다.
 * 그래야 토큰이 만료돼도 공개 API(지도 조회)가 계속 동작하고, 프론트가 /api/me로 만료를 감지해 갱신할 수 있다.
 */
public class CookieJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends Authentication> converter;

    public CookieJwtAuthenticationFilter(JwtDecoder jwtDecoder, Converter<Jwt, ? extends Authentication> converter) {
        this.jwtDecoder = jwtDecoder;
        this.converter = converter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization") == null
                ? TokenCookies.read(request, TokenCookies.ACCESS)
                : null;
        if (token != null) {
            try {
                Authentication authentication = converter.convert(jwtDecoder.decode(token));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException e) {
                // 익명으로 진행
            }
        }
        chain.doFilter(request, response);
    }
}
