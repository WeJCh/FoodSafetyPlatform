package com.mortal.user.config;

import com.mortal.user.service.AuthService;
import com.mortal.user.vo.AuthIntrospectVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserRequestAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public UserRequestAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(token)) {
                AuthIntrospectVO identity = authService.introspect(token);
                if (isActiveIdentity(identity)) {
                    AuthenticatedUser principal = new AuthenticatedUser(
                        identity.getUserId(),
                        identity.getUsername(),
                        identity.getUserType(),
                        identity.getRoles() == null ? List.of() : identity.getRoles()
                    );
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, token, toAuthorities(principal));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isActiveIdentity(AuthIntrospectVO identity) {
        if (identity == null || !identity.isValid() || identity.getUserId() == null) {
            return false;
        }
        boolean enabled = identity.getStatus() == null || Objects.equals(identity.getStatus(), 1);
        boolean notDeleted = identity.getDeleted() == null || !Objects.equals(identity.getDeleted(), 1);
        return enabled && notDeleted;
    }

    private Collection<SimpleGrantedAuthority> toAuthorities(AuthenticatedUser principal) {
        Set<String> authorities = new LinkedHashSet<>();
        if (principal.roles() != null) {
            authorities.addAll(principal.roles());
        }
        if (StringUtils.hasText(principal.userType())) {
            authorities.add(principal.userType());
        }
        List<SimpleGrantedAuthority> result = new ArrayList<>(authorities.size());
        for (String authority : authorities) {
            if (StringUtils.hasText(authority)) {
                result.add(new SimpleGrantedAuthority(authority));
            }
        }
        return result;
    }

    public record AuthenticatedUser(Long userId, String username, String userType, List<String> roles) {
    }
}
