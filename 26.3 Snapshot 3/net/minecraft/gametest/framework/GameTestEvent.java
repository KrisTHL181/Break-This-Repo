package net.minecraft.gametest.framework;

import org.jspecify.annotations.Nullable;

public class GameTestEvent {
   public final @Nullable Long expectedDelay;
   public final @Nullable Long minimumDelay;
   public final Runnable assertion;

   private GameTestEvent(final @Nullable Long expectedDelay, final @Nullable Long minimumDelay, final Runnable assertion) {
      this.expectedDelay = expectedDelay;
      this.minimumDelay = minimumDelay;
      this.assertion = assertion;
   }

   public static GameTestEvent create(final Runnable runnable) {
      return new GameTestEvent(null, null, runnable);
   }

   public static GameTestEvent create(final long expectedTick, final Runnable runnable) {
      return new GameTestEvent(expectedTick, null, runnable);
   }

   public static GameTestEvent createWithMinimumDelay(final long minimumDelay, final Runnable runnable) {
      return new GameTestEvent(null, minimumDelay, runnable);
   }
}
