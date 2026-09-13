package com.mojang.authlib.exceptions;

public class MinecraftClientException extends RuntimeException {
   protected final MinecraftClientException.ErrorType type;

   public MinecraftClientException(MinecraftClientException.ErrorType type, String message) {
      super(message);
      this.type = type;
   }

   public MinecraftClientException(MinecraftClientException.ErrorType type, String message, Throwable cause) {
      super(message, cause);
      this.type = type;
   }

   public MinecraftClientException.ErrorType getType() {
      return this.type;
   }

   public AuthenticationException toAuthenticationException() {
      return this.type == MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE ? new AuthenticationUnavailableException() : new AuthenticationException(this);
   }

   public enum ErrorType {
      SERVICE_UNAVAILABLE,
      HTTP_ERROR,
      JSON_ERROR;
   }
}
