package com.cathay.identify.security.models;

import com.cathay.identify.entity.AccountEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CustomAccountDetails implements UserDetails {
    private final AccountEntity account;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert role từ String sang GrantedAuthority
        return Collections.singleton(new SimpleGrantedAuthority(account.getRole().name()));
    }

    @Override
    public @Nullable String getPassword() {
        return account.getHash_password();
    }

    // login with email
    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // --- HÀM RIÊNG CỦA BẠN ĐỂ LẤY ID ---
    public String getAccountId() {
        return account.getId();
    }
}
