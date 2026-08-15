package com.securechat.auth;

import com.securechat.common.ApiException;
import com.securechat.conversation.ConversationService;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import com.securechat.user.UserSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ConversationService conversationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ConversationService conversationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.conversationService = conversationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String phoneNumber = request.phoneNumber().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number is already registered");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPhoneNumber(phoneNumber);
        user.setDisplayName(request.displayName().trim());
        user.setProfilePictureUrl(emptyToNull(request.profilePictureUrl()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setOnline(true);
        user.setLastSeen(Instant.now());
        AppUser saved = userRepository.save(user);
        return new AuthResponse(jwtService.generate(saved), UserSummaryResponse.self(saved));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.usernameOrPhone().trim().toLowerCase(Locale.ROOT))
                .or(() -> userRepository.findByPhoneNumber(request.usernameOrPhone().trim()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        user.setOnline(true);
        user.setLastSeen(Instant.now());
        return new AuthResponse(jwtService.generate(user), UserSummaryResponse.self(user));
    }

    @Transactional
    public void logout(AppUser user) {
        AppUser managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        conversationService.deleteTemporaryForUser(managed);
        managed.setOnline(false);
        managed.setLastSeen(Instant.now());
    }

    @Transactional
    public void changePassword(AppUser user, ChangePasswordRequest request) {
        AppUser managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), managed.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        managed.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
