package com.mojang.patchy;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.mojang.blocklist.BlockListSupplier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

@AutoService(BlockListSupplier.class)
public class MojangBlockListSupplier implements BlockListSupplier {
   @Nullable
   @Override
   public Predicate<String> createBlockList() {
      try {
         URLConnection urlConnection = new URL("https://sessionserver.mojang.com/blockedservers").openConnection();

         try (InputStream is = urlConnection.getInputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, BlockedServers.HASH_CHARSET));
            return new BlockedServers(reader.lines().collect(ImmutableSet.toImmutableSet()));
         }
      } catch (IOException var17) {
         return null;
      }
   }
}
