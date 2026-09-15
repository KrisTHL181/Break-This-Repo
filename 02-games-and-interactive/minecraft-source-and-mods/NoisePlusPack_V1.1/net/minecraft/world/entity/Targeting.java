package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

public interface Targeting {
   @Nullable LivingEntity getTarget();
}
