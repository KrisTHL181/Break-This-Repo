package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiSkinRenderState(
   PlayerModel playerModel,
   Identifier texture,
   float rotationX,
   float rotationY,
   float pivotY,
   int x0,
   int y0,
   int x1,
   int y1,
   float scale,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiSkinRenderState(
      PlayerModel p_453453_,
      Identifier p_458208_,
      float p_407583_,
      float p_407404_,
      float p_407776_,
      int p_410339_,
      int p_407942_,
      int p_406963_,
      int p_410743_,
      float p_408299_,
      @Nullable ScreenRectangle p_406864_
   ) {
      this(
         p_453453_,
         p_458208_,
         p_407583_,
         p_407404_,
         p_407776_,
         p_410339_,
         p_407942_,
         p_406963_,
         p_410743_,
         p_408299_,
         p_406864_,
         PictureInPictureRenderState.getBounds(p_410339_, p_407942_, p_406963_, p_410743_, p_406864_)
      );
   }
}
