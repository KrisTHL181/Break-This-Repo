package com.mojang.authlib.exceptions;

public class InsufficientPrivilegesException extends AuthenticationException {
   public InsufficientPrivilegesException() {
   }

   public InsufficientPrivilegesException(String message) {
      super(message);
   }

   public InsufficientPrivilegesException(String message, Throwable cause) {
      super(message, cause);
   }

   public InsufficientPrivilegesException(Throwable cause) {
      super(cause);
   }
}
