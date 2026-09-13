package com.mojang.authlib.minecraft;

public class InsecurePublicKeyException extends RuntimeException {
   public InsecurePublicKeyException(String message) {
      super(message);
   }

   public static class InvalidException extends InsecurePublicKeyException {
      public InvalidException(String message) {
         super(message);
      }
   }

   public static class MissingException extends InsecurePublicKeyException {
      public MissingException(String message) {
         super(message);
      }
   }
}
