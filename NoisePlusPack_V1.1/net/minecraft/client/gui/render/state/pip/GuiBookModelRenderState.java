package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiBookModelRenderState(
   BookModel bookModel,
   Identifier texture,
   float open,
   float flip,
   int x0,
   int y0,
   int x1,
   int y1,
   float scale,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiBookModelRenderState(
      BookModel p_458131_,
      Identifier p_458132_,
      float p_407276_,
      float p_409206_,
      int p_408693_,
      int p_408998_,
      int p_409659_,
      int p_408812_,
      float p_405937_,
      @Nullable ScreenRectangle p_410001_
   ) {
      this(
         p_458131_,
         p_458132_,
         p_407276_,
         p_409206_,
         p_408693_,
         p_408998_,
         p_409659_,
         p_408812_,
         p_405937_,
         p_410001_,
         PictureInPictureRenderState.getBounds(p_408693_, p_408998_, p_409659_, p_408812_, p_410001_)
      );
   }
}
