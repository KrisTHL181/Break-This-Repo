package com.mojang.blaze3d.platform;

import java.util.OptionalInt;

public record DisplayData(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean isFullscreen) {
   public DisplayData withSize(final int width, final int height) {
      return new DisplayData(width, height, this.fullscreenWidth, this.fullscreenHeight, this.isFullscreen);
   }

   public DisplayData withFullscreen(final boolean isFullscreen) {
      return new DisplayData(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, isFullscreen);
   }
}
