package com.securechat.settings;

public record ImportBackupResponse(
        int conversationsImported,
        int messagesImported
) {
}

