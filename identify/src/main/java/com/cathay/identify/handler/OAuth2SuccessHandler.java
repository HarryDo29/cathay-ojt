package com.cathay.identify.handler;

import com.cathay.identify.dto.response.account.AuthenticationResponse;
import com.cathay.identify.dto.response.refreshToken.RefreshTokenResponse;
import com.cathay.identify.entity.AccountEntity;
import com.cathay.identify.exception.AppException;
import com.cathay.identify.exception.ErrorCode;
import com.cathay.identify.repository.AccountRepository;
import com.cathay.identify.service.RefreshTokenImpl;
import com.cathay.identify.util.Cookie.CookieOption;
import com.cathay.identify.util.Cookie.CookieUtil;
import com.cathay.identify.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AccountRepository accRepo;
    private final RefreshTokenImpl rfSer;
    private final CookieUtil cookieUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = token.getPrincipal();
        String email = principal.getAttribute("email");

        // 1️⃣ Generate JWT
        AccountEntity account = accRepo.findAccountByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
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
        // 2️⃣ Redirect FE kèm token
        response.sendRedirect( "http://localhost:3000");
    }
}
