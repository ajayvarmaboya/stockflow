package com.stockflow.auth.application;

import com.stockflow.auth.api.AuthRequest;
import com.stockflow.auth.api.AuthResponse;
import com.stockflow.auth.api.RegisterRequest;
import com.stockflow.auth.domain.Role;
import com.stockflow.auth.domain.User;
import com.stockflow.auth.persistence.UserRepository;
import com.stockflow.common.exception.ConflictException;
import com.stockflow.common.exception.UnauthorizedException;
import com.stockflow.common.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("Username is already taken: " + request.getUsername(), "USERNAME_ALREADY_EXISTS");
        }

        Role role = Role.CUSTOMER;
        if (request.getRole() != null && "ADMIN".equalsIgnoreCase(request.getRole())) {
            role = Role.ADMIN;
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(
                UUID.randomUUID(),
                normalizedUsername,
                passwordHash,
                role,
                request.getFullName()
        );

        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        User user = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole().name());
    }
}
