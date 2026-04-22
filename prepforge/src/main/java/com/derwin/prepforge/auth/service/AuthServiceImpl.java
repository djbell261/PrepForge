package com.derwin.prepforge.auth.service;

import com.derwin.prepforge.auth.dto.AuthRequest;
import com.derwin.prepforge.auth.dto.AuthResponse;
import com.derwin.prepforge.auth.dto.RegisterRequest;
import com.derwin.prepforge.auth.entity.Role;
import com.derwin.prepforge.auth.entity.User;
import com.derwin.prepforge.auth.repository.UserRepository;
import com.derwin.prepforge.auth.security.JwtService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .token(jwtService.generateToken(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }
}
