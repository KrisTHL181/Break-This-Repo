package net.minecraft.client.renderer.state.gui.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jspecify.annotations.Nullable;

public record GuiBannerResultRenderState(
   BannerFlagModel flag,
   DyeColor baseColor,
   BannerPatternLayers resultBannerPatterns,
   int x0,
   int y0,
   int x1,
   int y1,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiBannerResultRenderState(
      final BannerFlagModel flag,
      final DyeColor baseColor,
      final BannerPatternLayers resultBannerPatterns,
      final int x0,
      final int y0,
      final int x1,
      final int y1,
      final @Nullable ScreenRectangle scissorArea
   ) {
      this(flag, baseColor, resultBannerPatterns, x0, y0, x1, y1, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
   }

   @Override
   public float scale() {
      return 16.0F;
   }
}
