package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LavaFogEnvironment extends FogEnvironment {
   private static final int COLOR = -6743808;

   @Override
   public int getBaseColor(ClientLevel p_406843_, Camera p_409568_, int p_407873_, float p_410246_) {
      return -6743808;
   }

   @Override
   public void setupFog(FogData p_406361_, Camera p_453178_, ClientLevel p_409030_, float p_410074_, DeltaTracker p_406706_) {
      if (p_453178_.entity().isSpectator()) {
         p_406361_.environmentalStart = -8.0F;
         p_406361_.environmentalEnd = p_410074_ * 0.5F;
      } else if (p_453178_.entity() instanceof LivingEntity livingentity && livingentity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         p_406361_.environmentalStart = 0.0F;
         p_406361_.environmentalEnd = 5.0F;
      } else {
         p_406361_.environmentalStart = 0.25F;
         p_406361_.environmentalEnd = 1.0F;
      }

      p_406361_.skyEnd = p_406361_.environmentalEnd;
      p_406361_.cloudEnd = p_406361_.environmentalEnd;
   }

   @Override
   public boolean isApplicable(@Nullable FogType p_408458_, Entity p_405922_) {
      return p_408458_ == FogType.LAVA;
   }
}
