package com.cathay.identify.controller;

import com.cathay.identify.dto.request.auth.LoginRequest;
import com.cathay.identify.dto.request.auth.RegisterRequest;
import com.cathay.identify.dto.response.ApiResponse;
import com.cathay.identify.dto.response.refreshToken.RefreshTokenResponse;
import com.cathay.identify.entity.AccountEntity;
import com.cathay.identify.service.AuthServiceImpl;
import com.cathay.identify.service.RefreshTokenImpl;
import com.cathay.identify.util.Cookie.CookieOption;
import com.cathay.identify.util.Cookie.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceImpl authSer;
    private final RefreshTokenImpl rfSer;
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public ApiResponse<AccountEntity> login (@RequestBody LoginRequest loginDto,
                                                      HttpServletResponse response){
        AccountEntity account = authSer.login(loginDto);
        RefreshTokenResponse tokens = rfSer.createRefreshToken(account.getId());
        // set access token & refresh token into cookie in response
        CookieOption acOption = CookieOption.builder()
                .httpOnly(false)
                .maxAge(15 * 60)
                .secure(false)
                .path("/")
                .build();
        cookieUtil.addTo(response, "access_token", tokens.getRefresh_token(), acOption);

        CookieOption rfOption = CookieOption.builder()
                .httpOnly(true)
                .maxAge(15 * 60)
                .secure(false)
                .path("/")
                .build();
        cookieUtil.addTo(response, "refresh_token", tokens.getRefresh_token(), rfOption);

        return ApiResponse.<AccountEntity>builder()
                .result(account)
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<AccountEntity> register (@RequestBody RegisterRequest registerDto,
                                                         HttpServletResponse response){
        AccountEntity account = authSer.register(registerDto);
        RefreshTokenResponse tokens = rfSer.createRefreshToken(account.getId());
        CookieOption acOption = CookieOption.builder()
                .httpOnly(false)
                .maxAge(15 * 60)
                .secure(false)
                .path("/")
                .build();
        cookieUtil.addTo(response, "access_token", tokens.getRefresh_token(), acOption);
        CookieOption rfOption = CookieOption.builder()
                .httpOnly(true)
                .maxAge(15 * 60)
                .secure(false)
                .path("/")
                .build();
        cookieUtil.addTo(response, "refresh_token", tokens.getRefresh_token(), rfOption);
        return ApiResponse.<AccountEntity>builder()
                .result(account)
                .build();
    }
}
