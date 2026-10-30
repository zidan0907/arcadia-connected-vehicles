package com.arcadia.auth.api.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "arcadia_access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "arcadia_refresh_token";

    @Value("${arcadia.security.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${arcadia.security.cookie.same-site:Lax}")
    private String sameSite;

    public void addAuthCookies(
            HttpServletResponse response,
            String accessToken,
            long accessMaxAgeSeconds,
            String refreshToken,
            long refreshMaxAgeSeconds
    ) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(accessMaxAgeSeconds)
                .sameSite(sameSite)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth")
                .maxAge(refreshMaxAgeSeconds)
                .sameSite(sameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite(sameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
    }

    public Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(val -> val != null && !val.isBlank())
                .findFirst();
    }
}