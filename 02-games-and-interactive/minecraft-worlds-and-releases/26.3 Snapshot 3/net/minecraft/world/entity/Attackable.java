package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

public interface Attackable {
   @Nullable LivingEntity getLastAttacker();
}
