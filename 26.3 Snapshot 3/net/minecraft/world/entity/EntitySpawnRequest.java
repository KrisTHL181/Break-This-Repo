package net.minecraft.world.entity;

public record EntitySpawnRequest(EntitySpawnReason reason, boolean ignoreChecks) {
}
