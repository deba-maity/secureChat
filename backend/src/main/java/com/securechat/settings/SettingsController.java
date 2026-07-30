package com.securechat.settings;

import com.securechat.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse get(@AuthenticationPrincipal AppUser currentUser) {
        return settingsService.get(currentUser);
    }

    @PutMapping
    public SettingsResponse update(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody UpdateSettingsRequest request
    ) {
        return settingsService.update(currentUser, request);
    }

    @PostMapping("/favorites/export")
    public FavoriteBackup exportFavorites(@AuthenticationPrincipal AppUser currentUser) {
        return settingsService.exportFavorites(currentUser);
    }

    @PostMapping("/favorites/import")
    public ImportBackupResponse importFavorites(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody FavoriteBackup backup
    ) {
        return settingsService.importFavorites(currentUser, backup);
    }

    @PostMapping("/favorite-pin/verify")
    public PinVerifyResponse verifyFavoritePin(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody PinVerifyRequest request
    ) {
        return settingsService.verifyFavoritePin(currentUser, request);
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AppUser currentUser) {
        settingsService.deleteAccount(currentUser);
        return ResponseEntity.noContent().build();
    }
}
