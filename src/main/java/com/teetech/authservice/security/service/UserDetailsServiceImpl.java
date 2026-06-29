package com.teetech.authservice.security.service;

import com.teetech.authservice.repository.UserRepository;
import com.teetech.authservice.security.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return repository.findByUsername(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> {
                    log.warn("User not found. username={}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }
}
