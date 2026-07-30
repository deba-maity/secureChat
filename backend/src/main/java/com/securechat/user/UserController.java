package com.securechat.user;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/users/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AppUser currentUser) {
        return UserSummaryResponse.self(currentUser);
    }

    @GetMapping("/api/users/search")
    public List<UserSummaryResponse> search(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam("q") @NotBlank String query
    ) {
        String normalized = query.trim();
        if (normalized.length() < 2) {
            return List.of();
        }
        return userRepository.search(normalized, currentUser.getId(), PageRequest.of(0, 12))
                .stream()
                .map(UserSummaryResponse::visibleToOthers)
                .toList();
    }
}

