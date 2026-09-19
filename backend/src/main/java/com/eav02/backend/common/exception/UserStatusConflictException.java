package com.eav02.backend.common.exception;

import com.eav02.backend.user.entity.AccountStatus;

public class UserStatusConflictException extends RuntimeException {

    private final AccountStatus currentStatus;

    public UserStatusConflictException(AccountStatus currentStatus) {
        super("La cuenta ya se encuentra en estado " + currentStatus.name());
        this.currentStatus = currentStatus;
    }

    public AccountStatus getCurrentStatus() {
        return currentStatus;
    }
}
