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
public class DarknessFogEnvironment extends MobEffectFogEnvironment {
   @Override
   public Holder<MobEffect> getMobEffect() {
      return MobEffects.DARKNESS;
   }

   @Override
   public void setupFog(FogData p_408959_, Camera p_452801_, ClientLevel p_409046_, float p_409573_, DeltaTracker p_408552_) {
      if (p_452801_.entity() instanceof LivingEntity livingentity) {
         MobEffectInstance mobeffectinstance = livingentity.getEffect(this.getMobEffect());
         if (mobeffectinstance != null) {
            float f = Mth.lerp(mobeffectinstance.getBlendFactor(livingentity, p_408552_.getGameTimeDeltaPartialTick(false)), p_409573_, 15.0F);
            p_408959_.environmentalStart = f * 0.75F;
            p_408959_.environmentalEnd = f;
            p_408959_.skyEnd = f;
            p_408959_.cloudEnd = f;
         }
      }
   }

   @Override
   public float getModifiedDarkness(LivingEntity p_409078_, float p_405888_, float p_406967_) {
      MobEffectInstance mobeffectinstance = p_409078_.getEffect(this.getMobEffect());
      return mobeffectinstance != null ? Math.max(mobeffectinstance.getBlendFactor(p_409078_, p_406967_), p_405888_) : p_405888_;
   }
}
