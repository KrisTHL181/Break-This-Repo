package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PowderedSnowFogEnvironment extends FogEnvironment {
   private static final int COLOR = -6308916;

   @Override
   public int getBaseColor(ClientLevel p_405825_, Camera p_407682_, int p_407729_, float p_405891_) {
      return -6308916;
   }

   @Override
   public void setupFog(FogData p_406185_, Camera p_451434_, ClientLevel p_406285_, float p_409283_, DeltaTracker p_409577_) {
      if (p_451434_.entity().isSpectator()) {
         p_406185_.environmentalStart = -8.0F;
         p_406185_.environmentalEnd = p_409283_ * 0.5F;
      } else {
         p_406185_.environmentalStart = 0.0F;
         p_406185_.environmentalEnd = 2.0F;
      }

      p_406185_.skyEnd = p_406185_.environmentalEnd;
      p_406185_.cloudEnd = p_406185_.environmentalEnd;
   }

   @Override
   public boolean isApplicable(@Nullable FogType p_409275_, Entity p_407159_) {
      return p_409275_ == FogType.POWDER_SNOW;
   }
}
