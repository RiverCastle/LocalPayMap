package com.localpaymap.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * 인가 요청(state 등)을 세션 대신 짧은 수명의 HttpOnly 쿠키에 보관해 서버를 stateless로 유지한다.
 * 쿠키는 서버 비밀키로 HMAC 서명하며, 서명이 맞을 때만 역직렬화하므로 위조된 값은 파싱되지 않는다.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final int MAX_AGE_SECONDS = 300;

    private final AuthProperties props;
    private final SecretKey key;

    public CookieOAuth2AuthorizationRequestRepository(AuthProperties props) {
        this.props = props;
        this.key = AuthConfig.secretKey(props);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String value = TokenCookies.read(request, COOKIE_NAME);
        return value == null ? null : verifyAndDeserialize(value);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            clear(request, response);
            return;
        }
        addCookie(response, serializeAndSign(authorizationRequest), MAX_AGE_SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest loaded = loadAuthorizationRequest(request);
        clear(request, response);
        return loaded;
    }

    void clear(HttpServletRequest request, HttpServletResponse response) {
        addCookie(response, "", 0);
    }

    private void addCookie(HttpServletResponse response, String value, int maxAge) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(props.cookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private String serializeAndSign(OAuth2AuthorizationRequest request) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(request);
            out.flush();
            byte[] payload = bytes.toByteArray();
            Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
            return enc.encodeToString(payload) + "." + enc.encodeToString(sign(payload));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private OAuth2AuthorizationRequest verifyAndDeserialize(String value) {
        try {
            String[] parts = value.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), signature)) {
                return null;
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
                return (OAuth2AuthorizationRequest) in.readObject();
            }
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            return null;
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
