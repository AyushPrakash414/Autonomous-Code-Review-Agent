package com.autonomousreview.service;

import com.autonomousreview.dto.auth.AuthResponse;
import com.autonomousreview.dto.auth.LoginRequest;
import com.autonomousreview.dto.auth.RegisterRequest;
import com.autonomousreview.dto.auth.UserDto;
import com.autonomousreview.exception.UserAlreadyExistsException;
import com.autonomousreview.model.AuthProvider;
import com.autonomousreview.model.Role;
import com.autonomousreview.model.User;
import com.autonomousreview.repository.UserRepository;
import com.autonomousreview.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email '" + request.email() + "' already exists");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                Role.ROLE_USER,
                AuthProvider.LOCAL
        );

        User savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(savedUser);

        return AuthResponse.of(jwtToken, jwtExpirationMs, UserDto.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.email()));

        String jwtToken = jwtService.generateToken(user);
        return AuthResponse.of(jwtToken, jwtExpirationMs, UserDto.fromEntity(user));
    }

    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("No authenticated user found in SecurityContext");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return UserDto.fromEntity(user);
    }
}
