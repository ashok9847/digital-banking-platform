package com.banking.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityHelper {

    private SecurityHelper() {}

    public static Optional<AuthenticatedUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().map(AuthenticatedUser::getId).orElse(null);
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public static boolean isAdminOrSupport() {
        return hasRole(SecurityConstants.ROLE_ADMIN) || hasRole(SecurityConstants.ROLE_SUPPORT);
    }
}
