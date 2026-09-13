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

public class DarknessFogEnvironment extends MobEffectFogEnvironment {
   @Override
   public Holder<MobEffect> getMobEffect() {
      return MobEffects.DARKNESS;
   }

   @Override
   public void setupFog(final FogData fog, final Camera camera, final ClientLevel level, final float renderDistance, final DeltaTracker deltaTracker) {
      if (camera.entity() instanceof LivingEntity livingEntity) {
         MobEffectInstance effect = livingEntity.getEffect(this.getMobEffect());
         if (effect != null) {
            float distance = Mth.lerp(effect.getBlendFactor(livingEntity, deltaTracker.getGameTimeDeltaPartialTick(false)), renderDistance, 15.0F);
            fog.environmentalStart = distance * 0.75F;
            fog.environmentalEnd = distance;
            fog.skyEnd = distance;
            fog.cloudEnd = distance;
         }
      }
   }

   @Override
   public float getModifiedDarkness(final LivingEntity entity, final float darkness, final float partialTickTime) {
      MobEffectInstance instance = entity.getEffect(this.getMobEffect());
      return instance != null ? Math.max(instance.getBlendFactor(entity, partialTickTime), darkness) : darkness;
   }
}
