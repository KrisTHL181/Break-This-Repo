package net.minecraft.server.jsonrpc.security;

import java.security.SecureRandom;

public record SecurityConfig(String secretKey) {
   private static final String SECRET_KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

   public static boolean isValid(final String secretKey) {
      return secretKey.isEmpty() ? false : secretKey.matches("^[a-zA-Z0-9]{40}$");
   }

   public static String generateSecretKey() {
      SecureRandom random = new SecureRandom();
      StringBuilder key = new StringBuilder(40);

      for (int i = 0; i < 40; i++) {
         key.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
               .charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length()))
         );
      }

      return key.toString();
   }
}
