package com.eav02.backend.common.exception;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> errors) {
}
