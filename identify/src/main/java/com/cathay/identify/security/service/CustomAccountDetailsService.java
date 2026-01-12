package com.cathay.identify.security.service;

import com.cathay.identify.entity.AccountEntity;
import com.cathay.identify.repository.AccountRepository;
import com.cathay.identify.security.models.CustomAccountDetails;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomAccountDetailsService implements UserDetailsService {
    private final AccountRepository accRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AccountEntity acc = accRepo.findAccountByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not exist"));

        return new CustomAccountDetails(acc);
    }
}
