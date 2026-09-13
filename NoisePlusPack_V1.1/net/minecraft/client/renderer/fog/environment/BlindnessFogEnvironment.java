package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlindnessFogEnvironment extends MobEffectFogEnvironment {
   @Override
   public Holder<MobEffect> getMobEffect() {
      return MobEffects.BLINDNESS;
   }

   @Override
   public void setupFog(FogData p_409503_, Camera p_453834_, ClientLevel p_408940_, float p_408868_, DeltaTracker p_408982_) {
      if (p_453834_.entity() instanceof LivingEntity livingentity) {
         MobEffectInstance mobeffectinstance = livingentity.getEffect(this.getMobEffect());
         if (mobeffectinstance != null) {
            float f = mobeffectinstance.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, mobeffectinstance.getDuration() / 20.0F), p_408868_, 5.0F);
            p_409503_.environmentalStart = f * 0.25F;
            p_409503_.environmentalEnd = f;
            p_409503_.skyEnd = f * 0.8F;
            p_409503_.cloudEnd = f * 0.8F;
         }
      }
   }

   @Override
   public float getModifiedDarkness(LivingEntity p_410191_, float p_405801_, float p_406906_) {
      MobEffectInstance mobeffectinstance = p_410191_.getEffect(this.getMobEffect());
      if (mobeffectinstance != null) {
         if (mobeffectinstance.endsWithin(19)) {
            p_405801_ = Math.max(mobeffectinstance.getDuration() / 20.0F, p_405801_);
         } else {
            p_405801_ = 1.0F;
         }
      }

      return p_405801_;
   }
}
