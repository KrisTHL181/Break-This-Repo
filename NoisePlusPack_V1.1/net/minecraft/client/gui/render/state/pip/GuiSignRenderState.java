package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.Model;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiSignRenderState(
   Model.Simple signModel,
   WoodType woodType,
   int x0,
   int y0,
   int x1,
   int y1,
   float scale,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiSignRenderState(
      Model.Simple p_431649_,
      WoodType p_409628_,
      int p_408801_,
      int p_410057_,
      int p_408779_,
      int p_406621_,
      float p_409611_,
      @Nullable ScreenRectangle p_409510_
   ) {
      this(
         p_431649_,
         p_409628_,
         p_408801_,
         p_410057_,
         p_408779_,
         p_406621_,
         p_409611_,
         p_409510_,
         PictureInPictureRenderState.getBounds(p_408801_, p_410057_, p_408779_, p_406621_, p_409510_)
      );
   }
}
