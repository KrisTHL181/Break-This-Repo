package net.minecraft.server.jsonrpc.security;

import java.security.SecureRandom;

public record SecurityConfig(String secretKey) {
   private static final String SECRET_KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

   public static boolean isValid(String p_430498_) {
      return p_430498_.isEmpty() ? false : p_430498_.matches("^[a-zA-Z0-9]{40}$");
   }

   public static String generateSecretKey() {
      SecureRandom securerandom = new SecureRandom();
      StringBuilder stringbuilder = new StringBuilder(40);

      for (int i = 0; i < 40; i++) {
         stringbuilder.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
               .charAt(securerandom.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length()))
         );
      }

      return stringbuilder.toString();
   }
}
