package com.capstone.ebookstore.service;

import com.capstone.ebookstore.dto.AuthDto;
import com.capstone.ebookstore.entity.User;
import com.capstone.ebookstore.exception.ConflictException;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.UserRepository;
import com.capstone.ebookstore.security.JwtTokenProvider;
import com.capstone.ebookstore.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtTokenProvider.generateToken(principal);
        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(principal);
        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(principal.getId())
                .email(principal.getEmail())
                .build();
    }

    public AuthDto.UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthDto.UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .giftPoints(user.getGiftPoints())
                .build();
    }
}
