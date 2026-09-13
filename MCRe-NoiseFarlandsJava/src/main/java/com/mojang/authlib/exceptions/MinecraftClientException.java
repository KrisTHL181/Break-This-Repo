package com.mojang.authlib.exceptions;

public class MinecraftClientException extends RuntimeException {

    public enum ErrorType {
        SERVICE_UNAVAILABLE,
        HTTP_ERROR,
        JSON_ERROR
    }

    protected final ErrorType type;

    protected MinecraftClientException(final ErrorType type, final String message) {
        super(message);
        this.type = type;
    }

    public MinecraftClientException(final ErrorType type, final String message, final Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }

    public AuthenticationException toAuthenticationException() {
        if (type == ErrorType.SERVICE_UNAVAILABLE) {
            return new AuthenticationUnavailableException();
        }
        return new AuthenticationException(this);
    }
}
