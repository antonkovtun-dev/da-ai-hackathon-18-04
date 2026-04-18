package com.example.chat.auth;

import com.example.chat.users.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AuthUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .map(u -> new AuthUserDetails(u.getId(), u.getEmail(), u.getPasswordHash()))
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
    }
}
