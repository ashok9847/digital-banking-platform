package com.banking.account.security;

import com.banking.common.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    private final String internalServiceKey;

    public InternalServiceAuthFilter(
            @Value("${internal.service.key:${INTERNAL_SERVICE_KEY:}}") String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (requestPath != null && requestPath.startsWith("/api/accounts/internal")) {
            String incomingKey = request.getHeader(SecurityConstants.INTERNAL_SERVICE_HEADER);

            if (StringUtils.hasText(incomingKey) && incomingKey.equals(internalServiceKey)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "internal-service",
                                null,
                                List.of(new SimpleGrantedAuthority(SecurityConstants.ROLE_INTERNAL_SERVICE))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
