package com.mojang.blaze3d.platform;

import java.util.OptionalInt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DisplayData(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean isFullscreen) {
   public DisplayData withSize(int p_393757_, int p_395466_) {
      return new DisplayData(p_393757_, p_395466_, this.fullscreenWidth, this.fullscreenHeight, this.isFullscreen);
   }

   public DisplayData withFullscreen(boolean p_391502_) {
      return new DisplayData(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, p_391502_);
   }
}
