package net.minecraft.gametest.framework;

import java.util.stream.Stream;
import net.minecraft.core.Holder;

@FunctionalInterface
public interface TestInstanceFinder {
    Stream<Holder.Reference<GameTestInstance>> findTests();
}