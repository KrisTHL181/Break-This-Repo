package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface EntityProcessor {
   EntityProcessor NOP = input -> input;

   @Nullable Entity process(Entity input);
}
