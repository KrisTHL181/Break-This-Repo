package net.minecraft.util.profiling.jfr;

import net.minecraft.server.MinecraftServer;

public enum Environment {
   CLIENT("client"),
   SERVER("server");

   private final String description;

   Environment(final String p_185276_) {
      this.description = p_185276_;
   }

   public static Environment from(MinecraftServer p_185279_) {
      return p_185279_.isDedicatedServer() ? SERVER : CLIENT;
   }

   public String getDescription() {
      return this.description;
   }
}
