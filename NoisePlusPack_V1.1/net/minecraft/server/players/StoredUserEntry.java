package net.minecraft.server.players;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

public abstract class StoredUserEntry<T> {
   private final @Nullable T user;

   public StoredUserEntry(@Nullable T p_11371_) {
      this.user = p_11371_;
   }

   public @Nullable T getUser() {
      return this.user;
   }

   boolean hasExpired() {
      return false;
   }

   protected abstract void serialize(JsonObject var1);
}
