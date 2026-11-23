package com.eav02.backend.statistics.exception;

public class InvalidStatisticsRequestException extends RuntimeException {

    private final String parameter;

    public InvalidStatisticsRequestException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public String parameter() {
        return parameter;
    }
}
