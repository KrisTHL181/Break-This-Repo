package net.minecraft.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientClockManager implements ClockManager {
    private final Map<Holder<WorldClock>, ClientClockManager.ClockInstance> clocks = new HashMap<>();
    private long lastTickGameTime;

    private ClientClockManager.ClockInstance getInstance(final Holder<WorldClock> definition) {
        return this.clocks.computeIfAbsent(definition, var0 -> new ClientClockManager.ClockInstance());
    }

    public void tick(final long gameTime) {
        long gameTimeDelta = gameTime - this.lastTickGameTime;
        this.lastTickGameTime = gameTime;

        for (ClientClockManager.ClockInstance instance : this.clocks.values()) {
            double newPartialTicks = instance.partialTick + (double)gameTimeDelta * instance.rate;
            long fullTicks = Mth.floor(newPartialTicks);
            instance.partialTick = (float)(newPartialTicks - fullTicks);
            instance.totalTicks += fullTicks;
        }
    }

    public void handleUpdates(final long gameTime, final Map<Holder<WorldClock>, ClockNetworkState> updates) {
        this.tick(gameTime);
        updates.forEach((definition, state) -> {
            ClientClockManager.ClockInstance clock = this.getInstance((Holder<WorldClock>)definition);
            clock.totalTicks = state.totalTicks();
            clock.partialTick = state.partialTick();
            clock.rate = state.rate();
        });
    }

    @Override
    public long getTotalTicks(final Holder<WorldClock> definition) {
        return this.getInstance(definition).totalTicks;
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClockInstance {
        private long totalTicks;
        private float partialTick;
        private float rate = 1.0F;
    }
}