package net.minecraft.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.ClockInstance;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;

public class ClientClockManager implements ClockManager {
   private final Map<Holder<WorldClock>, ClientClockManager.ClientClockInstance> clocks = new HashMap<>();
   private long lastTickGameTime;

   public ClientClockManager.ClientClockInstance getInstance(final Holder<WorldClock> definition) {
      return this.clocks.computeIfAbsent(definition, var0 -> new ClientClockManager.ClientClockInstance());
   }

   public void tick(final long gameTime) {
      long gameTimeDelta = gameTime - this.lastTickGameTime;
      this.lastTickGameTime = gameTime;

      for (ClientClockManager.ClientClockInstance instance : this.clocks.values()) {
         double newPartialTicks = instance.partialTick + (double)gameTimeDelta * instance.rate;
         long fullTicks = Mth.floor(newPartialTicks);
         instance.partialTick = (float)(newPartialTicks - fullTicks);
         instance.totalTicks += fullTicks;
      }
   }

   public void handleUpdates(final long gameTime, final Map<Holder<WorldClock>, ClockNetworkState> updates) {
      this.tick(gameTime);
      updates.forEach((definition, state) -> {
         ClientClockManager.ClientClockInstance clock = this.getInstance((Holder<WorldClock>)definition);
         clock.totalTicks = state.totalTicks();
         clock.partialTick = state.partialTick();
         clock.rate = state.rate();
      });
   }

   public static class ClientClockInstance implements ClockInstance {
      private long totalTicks;
      private float partialTick;
      private float rate = 1.0F;

      @Override
      public long totalTicks() {
         return this.totalTicks;
      }

      @Override
      public float partialTick() {
         return this.partialTick;
      }

      @Override
      public float rate() {
         return this.rate;
      }

      @Override
      public boolean isPaused() {
         return this.rate == 0.0F;
      }
   }
}
