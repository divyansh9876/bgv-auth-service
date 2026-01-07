package com.bgv.auth.dto;

/**
 * Defines system-wide user roles.
 * Used across all microservices for authorization.
 */
public enum UserRole {
    USER,       // Regular user
    ADMIN;      // Internal admin

    /**
     * Helper method for Spring Security role mapping.
     */
    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
