package net.minecraft.gametest.framework;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;

@FunctionalInterface
public interface TestPosFinder {
    Stream<BlockPos> findTestPos();
}