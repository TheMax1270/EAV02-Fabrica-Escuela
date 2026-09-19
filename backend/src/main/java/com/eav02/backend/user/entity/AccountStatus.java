package com.eav02.backend.user.entity;

public enum AccountStatus {
    ACTIVE,
    SUSPENDED;

    public static AccountStatus of(boolean enabled) {
        return enabled ? ACTIVE : SUSPENDED;
    }
}
