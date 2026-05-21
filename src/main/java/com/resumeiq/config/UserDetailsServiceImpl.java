package com.resumeiq.config;

import com.resumeiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Standalone bean — lives outside SecurityConfig so that:
 *   - JwtAuthenticationFilter can inject it directly
 *   - SecurityConfig can also inject it
 *   - Neither SecurityConfig nor JwtAuthenticationFilter depend on each other
 *
 * This breaks the circular reference:
 *   BEFORE: JwtAuthFilter → SecurityConfig → JwtAuthFilter (cycle!)
 *   AFTER:  JwtAuthFilter → UserDetailsServiceImpl  (no cycle)
 *           SecurityConfig → UserDetailsServiceImpl  (no cycle)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email
                ));
    }
}