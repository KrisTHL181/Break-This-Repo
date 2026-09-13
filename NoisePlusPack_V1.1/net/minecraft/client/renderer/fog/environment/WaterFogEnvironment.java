package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WaterFogEnvironment extends FogEnvironment {
   @Override
   public void setupFog(FogData p_406618_, Camera p_460285_, ClientLevel p_406286_, float p_408021_, DeltaTracker p_407294_) {
      float f = p_407294_.getGameTimeDeltaPartialTick(false);
      p_406618_.environmentalStart = p_460285_.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_START_DISTANCE, f);
      p_406618_.environmentalEnd = p_460285_.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_END_DISTANCE, f);
      if (p_460285_.entity() instanceof LocalPlayer localplayer) {
         p_406618_.environmentalEnd = p_406618_.environmentalEnd * Math.max(0.25F, localplayer.getWaterVision());
      }

      p_406618_.skyEnd = p_406618_.environmentalEnd;
      p_406618_.cloudEnd = p_406618_.environmentalEnd;
   }

   @Override
   public boolean isApplicable(@Nullable FogType p_409990_, Entity p_406892_) {
      return p_409990_ == FogType.WATER;
   }

   @Override
   public int getBaseColor(ClientLevel p_409944_, Camera p_407580_, int p_409596_, float p_409082_) {
      return p_407580_.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_COLOR, p_409082_);
   }
}
