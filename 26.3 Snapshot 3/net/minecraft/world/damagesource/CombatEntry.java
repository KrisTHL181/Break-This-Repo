package net.minecraft.world.damagesource;

import org.jspecify.annotations.Nullable;

public record CombatEntry(DamageSource source, float damage, @Nullable FallLocation fallLocation, float fallDistance) {
}
