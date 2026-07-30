package com.securechat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            JwtStompChannelInterceptor jwtStompChannelInterceptor,
            @Value("${securechat.cors.allowed-origins}") String allowedOrigins
    ) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
        this.allowedOriginPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
