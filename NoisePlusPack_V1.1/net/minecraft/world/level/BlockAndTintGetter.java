package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.LevelLightEngine;

public interface BlockAndTintGetter extends BlockGetter {
   float getShade(Direction var1, boolean var2);

   LevelLightEngine getLightEngine();

   int getBlockTint(BlockPos var1, ColorResolver var2);

   default int getBrightness(LightLayer p_45518_, BlockPos p_45519_) {
      return this.getLightEngine().getLayerListener(p_45518_).getLightValue(p_45519_);
   }

   default int getRawBrightness(BlockPos p_45525_, int p_45526_) {
      return this.getLightEngine().getRawBrightness(p_45525_, p_45526_);
   }

   default boolean canSeeSky(BlockPos p_45528_) {
      return this.getBrightness(LightLayer.SKY, p_45528_) >= 15;
   }
}
