package net.minecraft.world.clock;

import net.minecraft.core.Holder;

public interface ClockManager {
    long getTotalTicks(Holder<WorldClock> definition);
}