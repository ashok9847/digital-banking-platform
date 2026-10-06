package com.banking.common.security;

public final class SecurityConstants {

    private SecurityConstants() {
        // utility class
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPPORT = "ROLE_SUPPORT";
    public static final String ROLE_INTERNAL_SERVICE = "ROLE_INTERNAL_SERVICE";

    public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service-Key";
}
