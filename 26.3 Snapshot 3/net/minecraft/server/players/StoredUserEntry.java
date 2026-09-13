package net.minecraft.server.players;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

public abstract class StoredUserEntry<T> {
   private final @Nullable T user;

   public StoredUserEntry(final @Nullable T user) {
      this.user = user;
   }

   public @Nullable T getUser() {
      return this.user;
   }

   public boolean hasExpired() {
      return false;
   }

   protected abstract void serialize(final JsonObject object);
}
