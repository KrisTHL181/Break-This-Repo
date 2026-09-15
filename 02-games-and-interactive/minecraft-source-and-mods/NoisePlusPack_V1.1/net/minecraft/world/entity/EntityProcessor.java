package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface EntityProcessor {
   EntityProcessor NOP = p_454440_ -> p_454440_;

   @Nullable Entity process(Entity var1);
}
