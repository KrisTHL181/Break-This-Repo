package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

public abstract class FogEnvironment {
   public abstract void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker);

   public boolean providesColor() {
      return true;
   }

   public int getBaseColor(final ClientLevel level, final Camera camera, final int renderDistance, final float partialTicks) {
      return -1;
   }

   public boolean modifiesDarkness() {
      return false;
   }

   public float getModifiedDarkness(final LivingEntity entity, final float darkness, final float partialTickTime) {
      return darkness;
   }

   public abstract boolean isApplicable(@Nullable FogType fogType, Entity entity);
}
