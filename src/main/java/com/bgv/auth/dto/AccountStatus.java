package com.bgv.auth.dto;

/**
 * Represents the lifecycle status of a user account.
 */
public enum AccountStatus {
    ACTIVE,     // Normal active account
    BLOCKED;    // Blocked by admin

    public boolean isActive() {
        return this == ACTIVE;
    }
}
