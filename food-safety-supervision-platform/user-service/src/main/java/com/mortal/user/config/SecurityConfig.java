package com.mortal.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.user.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final UserRequestAuthenticationFilter userRequestAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(UserRequestAuthenticationFilter userRequestAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.userRequestAuthenticationFilter = userRequestAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/verify",
                    "/api/users/register/public",
                    "/api/users/register/enterprise",
                    "/api/internal/users/**",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                .requestMatchers(
                    "/api/auth/logout",
                    "/api/auth/introspect",
                    "/api/users/me",
                    "/api/users/me/audit-logs",
                    "/api/users/me/password"
                ).authenticated()
                .requestMatchers(
                    "/api/admin/**",
                    "/api/roles/**",
                    "/api/users/*"
                ).hasAuthority("ADMIN")
                .anyRequest().denyAll()
            )
            .addFilterBefore(userRequestAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, ex) ->
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED)
                )
                .accessDeniedHandler((request, response, ex) ->
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN)
                )
            );
        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(errorCode.code(), errorCode.message()));
    }
}
