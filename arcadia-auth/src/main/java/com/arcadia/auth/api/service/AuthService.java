package com.arcadia.auth.api.service;

import com.arcadia.auth.api.dto.AuthResponse;
import com.arcadia.auth.api.dto.LoginRequest;
import com.arcadia.auth.api.dto.RefreshTokenRequest;
import com.arcadia.auth.api.dto.SignupRequest;
import com.arcadia.auth.api.dto.SignupResponse;
import com.arcadia.auth.api.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    SignupResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    AuthResponse refreshToken(
            RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );

    void logout(
            String authorizationHeader,
            RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );

    UserProfileResponse getCurrentUserProfile(String userId);
}