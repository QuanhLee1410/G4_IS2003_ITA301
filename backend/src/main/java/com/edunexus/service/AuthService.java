package com.edunexus.service;

import com.edunexus.dto.AuthResponse;
import com.edunexus.dto.LoginRequest;
import com.edunexus.dto.RegisterRequest;
import com.edunexus.entity.Role;
import com.edunexus.entity.User;
import com.edunexus.exception.AppException;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.RoleRepository;
import com.edunexus.repository.UserRepository;
import com.edunexus.security.JwtProvider;
import com.edunexus.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtProvider jwtProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email is already registered");
        }

        Role role = roleRepository.findByRoleName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        user = userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user.getUserId(), user.getUsername(), role.getRoleName());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());

        log.info("User registered successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(role.getRoleName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());

            log.info("User logged in successfully: {}", user.getUsername());

            return AuthResponse.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().getRoleName())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            throw new AppException("Invalid username or password");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AppException("Invalid or expired refresh token");
        }

        Integer userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String newAccessToken = jwtProvider.generateAccessToken(user.getUserId(), user.getUsername(), user.getRole().getRoleName());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getUserId());

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
