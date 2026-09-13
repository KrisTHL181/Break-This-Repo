package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PanoramicScreenshotParameters;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AtmosphericFogEnvironment extends FogEnvironment {
   private static final int MIN_RAIN_FOG_SKY_LIGHT = 8;
   private static final float RAIN_FOG_START_OFFSET = -160.0F;
   private static final float RAIN_FOG_END_OFFSET = -256.0F;
   private float rainFogMultiplier;

   @Override
   public int getBaseColor(ClientLevel p_453620_, Camera p_452594_, int p_458740_, float p_458449_) {
      int i = p_452594_.attributeProbe().getValue(EnvironmentAttributes.FOG_COLOR, p_458449_);
      if (p_458740_ >= 4) {
         float f = p_452594_.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, p_458449_) * (float) (Math.PI / 180.0);
         float f1 = Mth.sin(f) > 0.0F ? -1.0F : 1.0F;
         PanoramicScreenshotParameters panoramicscreenshotparameters = Minecraft.getInstance().gameRenderer.getPanoramicScreenshotParameters();
         Vector3fc vector3fc = panoramicscreenshotparameters != null ? panoramicscreenshotparameters.forwardVector() : p_452594_.forwardVector();
         float f2 = vector3fc.dot(f1, 0.0F, 0.0F);
         if (f2 > 0.0F) {
            int j = p_452594_.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, p_458449_);
            float f3 = ARGB.alphaFloat(j);
            if (f3 > 0.0F) {
               i = ARGB.srgbLerp(f2 * f3, i, ARGB.opaque(j));
            }
         }
      }

      int k = p_452594_.attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, p_458449_);
      k = applyWeatherDarken(k, p_453620_.getRainLevel(p_458449_), p_453620_.getThunderLevel(p_458449_));
      float f4 = Math.min(p_452594_.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, p_458449_) / 16.0F, p_458740_);
      float f5 = Mth.clampedLerp(f4 / 32.0F, 0.25F, 1.0F);
      f5 = 1.0F - (float)Math.pow(f5, 0.25);
      return ARGB.srgbLerp(f5, i, k);
   }

   private static int applyWeatherDarken(int p_451734_, float p_460930_, float p_460406_) {
      if (p_460930_ > 0.0F) {
         float f = 1.0F - p_460930_ * 0.5F;
         float f1 = 1.0F - p_460930_ * 0.4F;
         p_451734_ = ARGB.scaleRGB(p_451734_, f, f, f1);
      }

      if (p_460406_ > 0.0F) {
         p_451734_ = ARGB.scaleRGB(p_451734_, 1.0F - p_460406_ * 0.5F);
      }

      return p_451734_;
   }

   @Override
   public void setupFog(FogData p_407178_, Camera p_459302_, ClientLevel p_410621_, float p_410424_, DeltaTracker p_409623_) {
      this.updateRainFogState(p_459302_, p_410621_, p_409623_);
      float f = p_409623_.getGameTimeDeltaPartialTick(false);
      p_407178_.environmentalStart = p_459302_.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, f);
      p_407178_.environmentalEnd = p_459302_.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, f);
      p_407178_.environmentalStart = p_407178_.environmentalStart + -160.0F * this.rainFogMultiplier;
      float f1 = Math.min(96.0F, p_407178_.environmentalEnd);
      p_407178_.environmentalEnd = Math.max(f1, p_407178_.environmentalEnd + -256.0F * this.rainFogMultiplier);
      p_407178_.skyEnd = Math.min(p_410424_, p_459302_.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, f));
      p_407178_.cloudEnd = Math.min(
         Minecraft.getInstance().options.cloudRange().get() * 16, p_459302_.attributeProbe().getValue(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE, f)
      );
      if (Minecraft.getInstance().gui.getBossOverlay().shouldCreateWorldFog()) {
         p_407178_.environmentalStart = Math.min(p_407178_.environmentalStart, 10.0F);
         p_407178_.environmentalEnd = Math.min(p_407178_.environmentalEnd, 96.0F);
         p_407178_.skyEnd = p_407178_.environmentalEnd;
         p_407178_.cloudEnd = p_407178_.environmentalEnd;
      }
   }

   private void updateRainFogState(Camera p_457860_, ClientLevel p_460637_, DeltaTracker p_456427_) {
      BlockPos blockpos = p_457860_.blockPosition();
      Biome biome = p_460637_.getBiome(blockpos).value();
      float f = p_456427_.getGameTimeDeltaTicks();
      float f1 = p_456427_.getGameTimeDeltaPartialTick(false);
      boolean flag = biome.hasPrecipitation();
      float f2 = Mth.clamp((p_460637_.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(blockpos) - 8.0F) / 7.0F, 0.0F, 1.0F);
      float f3 = p_460637_.getRainLevel(f1) * f2 * (flag ? 1.0F : 0.5F);
      this.rainFogMultiplier = this.rainFogMultiplier + (f3 - this.rainFogMultiplier) * f * 0.2F;
   }

   @Override
   public boolean isApplicable(@Nullable FogType p_407495_, Entity p_406898_) {
      return p_407495_ == FogType.ATMOSPHERIC;
   }
}
