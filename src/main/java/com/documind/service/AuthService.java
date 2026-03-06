package com.documind.service;

import com.documind.model.User;
import com.documind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }

    public String register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken: " + username);
        }
        if (username == null || username.trim().length() < 3) {
            throw new RuntimeException("Username must be at least 3 characters");
        }
        if (password == null || password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(hashedPassword);
        newUser.setRole("ROLE_USER");

        userRepository.save(newUser);
        return "Registration successful! You can now log in.";
    }
}
