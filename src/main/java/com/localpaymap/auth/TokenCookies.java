package com.localpaymap.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 토큰은 JS에서 읽을 수 없도록 HttpOnly 쿠키로만 내려준다. refresh 쿠키는 갱신/로그아웃 경로로만 전송된다. */
@Component
public class TokenCookies {

    public static final String ACCESS = "ACCESS_TOKEN";
    public static final String REFRESH = "REFRESH_TOKEN";
    public static final String REFRESH_PATH = "/api/auth";

    private final AuthProperties props;

    public TokenCookies(AuthProperties props) {
        this.props = props;
    }

    public void write(HttpServletResponse response, TokenPair pair) {
        add(response, build(ACCESS, pair.accessToken(), "/", props.jwt().accessTtl()));
        add(response, build(REFRESH, pair.refreshToken(), REFRESH_PATH, props.jwt().refreshTtl()));
    }

    public void clear(HttpServletResponse response) {
        add(response, build(ACCESS, "", "/", Duration.ZERO));
        add(response, build(REFRESH, "", REFRESH_PATH, Duration.ZERO));
    }

    public static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName()) && !c.getValue().isEmpty()) {
                return c.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private static void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
