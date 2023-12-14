package com.ocow.back.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.ocow.back.security.JwtUtil;
import com.ocow.back.security.UserDetailsServiceImpl;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	@Autowired
	private JwtUtil jwtTokenUtil;
	
	@Autowired
	private UserDetailsServiceImpl userDetailsService;
	
	 	@Override
	    public void registerStompEndpoints(StompEndpointRegistry registry) {
	        registry.addEndpoint("/socket")
	                .setAllowedOriginPatterns("*")
	                .withSockJS();
	        registry.addEndpoint("/ws");
	    }

	    @Override
	    public void configureMessageBroker(MessageBrokerRegistry registry) {
	        registry.setApplicationDestinationPrefixes("/app")
	                .enableSimpleBroker("/ws");
	    }
	    
	    @Override
	    public void configureClientInboundChannel(ChannelRegistration registration) {
	    	System.out.println("configureClientInboundChannel");
	        registration.interceptors(new ChannelInterceptor() {
	            @Override
	            public Message<?> preSend(Message<?> message, MessageChannel channel) {
	                StompHeaderAccessor accessor =
	                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
	                System.out.println("Headers: "+accessor);

	                assert accessor != null;
	                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

	                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
	                    assert authorizationHeader != null;
	                    String token = authorizationHeader.substring(6);

	                    String username = jwtTokenUtil.extractUsername(token);
	                    System.out.println("username: "+username);
	                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
	                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

	                    accessor.setUser(usernamePasswordAuthenticationToken);
	                    accessor.setLeaveMutable(true);
	                }

	                return message;
	            }

	        });
	    }
}
