package com.cathay.identify.service;

import com.cathay.identify.dto.response.refreshToken.RefreshTokenResponse;
import com.cathay.identify.entity.AccountEntity;
import com.cathay.identify.entity.RefreshTokenEntity;
import com.cathay.identify.exception.AppException;
import com.cathay.identify.exception.ErrorCode;
import com.cathay.identify.interfaces.RefreshTokenService;
import com.cathay.identify.properties.RefreshTokenProperties;
import com.cathay.identify.repository.AccountRepository;
import com.cathay.identify.repository.RefreshTokenRepository;
import com.cathay.identify.security.util.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class RefreshTokenImpl implements RefreshTokenService {
    private final JwtUtil jwtUtil;
    private final AccountRepository accRepo;
    private final RefreshTokenRepository rfRepo;
    private final RefreshTokenProperties rfProps;


    @Override
    public RefreshTokenResponse createRefreshToken(String account_id) {
        AccountEntity acc = accRepo.findById(account_id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Map<String, String> claim = Map.of(
                "email", acc.getEmail(),
                "role", acc.getRole().name());
        String access_token = jwtUtil.buildToken(claim, account_id);
        String refresh_token = jwtUtil.buildToken(claim, account_id, rfProps.getExpire(), rfProps.getSecret());
        RefreshTokenEntity rf = RefreshTokenEntity.builder()
                .token(refresh_token)
                .account(acc)
                .build();
        rfRepo.save(rf);
        return RefreshTokenResponse.builder()
                .access_token(access_token)
                .refresh_token(refresh_token)
                .build();
    }

    @Override
    public RefreshTokenResponse refreshAccessToken(String account_id) {
        return this.createRefreshToken(account_id);
    }
}
