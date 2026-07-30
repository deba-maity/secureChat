package com.securechat.config;

import java.security.Principal;

public record StompUserPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
