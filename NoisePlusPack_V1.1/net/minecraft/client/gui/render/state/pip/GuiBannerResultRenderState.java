package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
      BannerFlagModel p_457287_,
      DyeColor p_406149_,
      BannerPatternLayers p_409815_,
      int p_406594_,
      int p_405943_,
      int p_407471_,
      int p_406060_,
      @Nullable ScreenRectangle p_410304_
   ) {
      this(
         p_457287_,
         p_406149_,
         p_409815_,
         p_406594_,
         p_405943_,
         p_407471_,
         p_406060_,
         p_410304_,
         PictureInPictureRenderState.getBounds(p_406594_, p_405943_, p_407471_, p_406060_, p_410304_)
      );
   }

   @Override
   public float scale() {
      return 16.0F;
   }
}
