package com.cathay.identify.security.service;

import com.cathay.identify.entity.AccountEntity;
import com.cathay.identify.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AccountRepository accRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(req);
        System.out.println("OAuth2User: " + oAuth2User);
        // Google attributes
        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        String avtUrl = oAuth2User.getAttribute("picture");
        String google_id = oAuth2User.getAttribute("sub");

        // 1️⃣ Check user
        AccountEntity account = accRepo.findAccountByEmail(email)
                .orElseGet(() -> {
                    // 2️⃣ Create user
                    AccountEntity newUser = AccountEntity.builder()
                            .email(email)
                            .name(name)
                            .googleId(google_id)
                            .avtUrl(avtUrl)
                            .build();
                    return accRepo.save(newUser);
                });
        // 3️⃣ Attach domain user (OPTIONAL)
        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}
