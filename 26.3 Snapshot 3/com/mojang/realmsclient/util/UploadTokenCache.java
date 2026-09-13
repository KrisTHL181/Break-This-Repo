package com.mojang.realmsclient.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

public class UploadTokenCache {
   private static final Long2ObjectMap<String> TOKEN_CACHE = new Long2ObjectOpenHashMap();

   public static String get(final long realmId) {
      return (String)TOKEN_CACHE.get(realmId);
   }

   public static void invalidate(final long realmId) {
      TOKEN_CACHE.remove(realmId);
   }

   public static void put(final long realmId, final @Nullable String token) {
      TOKEN_CACHE.put(realmId, token);
   }
}
