package com.coworkinghub.coworkinghub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /**
     * Anything that changes data or reads private data always needs a token; who may do what is checked in
     * the services (see AccessService). When false (the default) the public catalogue (spaces, rooms,
     * equipment) can be browsed without a token; true requires a token for that too.
     */
    @Value("${app.security.require-auth:false}")
    private boolean requireAuth;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) ->
                        SecurityResponses.writeUnauthorized(response, "Authentication required")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**", "/error").permitAll();
                    if (!requireAuth) {
                        auth.requestMatchers(HttpMethod.GET,
                                "/api/coworking-spaces", "/api/coworking-spaces/", "/api/coworking-spaces/*",
                                "/api/coworking-spaces/*/rooms", "/api/coworking-spaces/*/equipment").permitAll();
                    }
                    auth.anyRequest().authenticated();
                });
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
