package net.minecraft.util.debug;

import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public interface DebugValueSource {
   void registerDebugValues(ServerLevel var1, DebugValueSource.Registration var2);

   interface Registration {
      <T> void register(DebugSubscription<T> var1, DebugValueSource.ValueGetter<T> var2);
   }

   interface ValueGetter<T> {
      @Nullable T get();
   }
}
