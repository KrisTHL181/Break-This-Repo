package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class FogEnvironment {
   public abstract void setupFog(FogData var1, Camera var2, ClientLevel var3, float var4, DeltaTracker var5);

   public boolean providesColor() {
      return true;
   }

   public int getBaseColor(ClientLevel p_408834_, Camera p_407835_, int p_410065_, float p_407737_) {
      return -1;
   }

   public boolean modifiesDarkness() {
      return false;
   }

   public float getModifiedDarkness(LivingEntity p_408428_, float p_405830_, float p_410252_) {
      return p_405830_;
   }

   public abstract boolean isApplicable(@Nullable FogType var1, Entity var2);
}
