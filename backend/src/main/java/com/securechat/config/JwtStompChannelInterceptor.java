package com.securechat.config;

import com.securechat.auth.JwtService;
import com.securechat.user.AppUser;
import com.securechat.user.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtStompChannelInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = bearerToken(accessor);
            String username = jwtService.subject(token);
            AppUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AccessDeniedException("Invalid WebSocket token"));
            user.setOnline(true);
            user.setLastSeen(Instant.now());
            userRepository.save(user);
            accessor.setUser(new StompUserPrincipal(user.getUsername()));
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand()) && accessor.getUser() != null) {
            userRepository.findByUsername(accessor.getUser().getName()).ifPresent(user -> {
                user.setOnline(false);
                user.setLastSeen(Instant.now());
                userRepository.save(user);
            });
        }
        return message;
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty() || !headers.get(0).startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing WebSocket token");
        }
        return headers.get(0).substring(7);
    }
}
