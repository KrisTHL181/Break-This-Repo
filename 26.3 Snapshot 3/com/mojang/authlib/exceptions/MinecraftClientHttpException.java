package com.mojang.authlib.exceptions;

import com.mojang.authlib.services.response.ErrorResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.StringJoiner;
import org.jspecify.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;

public class MinecraftClientHttpException extends MinecraftClientException {
   public static final int UNAUTHORIZED = 401;
   public static final int FORBIDDEN = 403;
   private final int status;
   @Nullable
   private final ErrorResponse response;
   @Nullable
   private final Duration retryAfter;

   public MinecraftClientHttpException(int status) {
      this(status, null, null);
   }

   public MinecraftClientHttpException(int status, ErrorResponse response) {
      this(status, response, null);
   }

   public MinecraftClientHttpException(int status, @Nullable ErrorResponse response, @Nullable Duration retryAfter) {
      super(MinecraftClientException.ErrorType.HTTP_ERROR, getErrorMessage(status, response));
      this.status = status;
      this.response = response;
      this.retryAfter = retryAfter;
   }

   public int getStatus() {
      return this.status;
   }

   public Optional<ErrorResponse> getResponse() {
      return Optional.ofNullable(this.response);
   }

   public Optional<Duration> getRetryAfter() {
      return Optional.ofNullable(this.retryAfter);
   }

   @Override
   public String toString() {
      return new StringJoiner(", ", MinecraftClientHttpException.class.getSimpleName() + "[", "]")
         .add("type=" + this.type)
         .add("status=" + this.status)
         .add("response=" + this.response)
         .toString();
   }

   @Override
   public AuthenticationException toAuthenticationException() {
      if (this.hasError("ForbiddenOperationException")) {
         return new InvalidCredentialsException(this.getMessage());
      } else if (this.hasError("multiplayer.access.banned")) {
         return new UserBannedException();
      } else if (this.hasError("FORCED_USERNAME_CHANGE")) {
         return new ForcedUsernameChangeException();
      } else if (this.hasError("InsufficientPrivilegesException")) {
         return new InsufficientPrivilegesException(this.getMessage(), this);
      } else if (this.status == 401) {
         return new InvalidCredentialsException(this.getMessage(), this);
      } else {
         return this.status >= 500 ? new AuthenticationUnavailableException(this.getMessage(), this) : new AuthenticationException(this.getMessage(), this);
      }
   }

   private Optional<String> getError() {
      return this.getResponse().map(ErrorResponse::error).filter(StringUtils::isNotEmpty);
   }

   private static String getErrorMessage(int status, ErrorResponse response) {
      String errorMessage;
      if (response != null) {
         if (StringUtils.isNotEmpty(response.errorMessage())) {
            errorMessage = response.errorMessage();
         } else if (StringUtils.isNotEmpty(response.error())) {
            errorMessage = response.error();
         } else {
            errorMessage = "Status: " + status;
         }
      } else {
         errorMessage = "Status: " + status;
      }

      return errorMessage;
   }

   private boolean hasError(String error) {
      return this.getError().filter(value -> value.equalsIgnoreCase(error)).isPresent();
   }
}
