package net.minecraft.client.renderer.state.gui.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

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
      final BookModel bookModel,
      final Identifier texture,
      final float open,
      final float flip,
      final int x0,
      final int y0,
      final int x1,
      final int y1,
      final float scale,
      final @Nullable ScreenRectangle scissorArea
   ) {
      this(bookModel, texture, open, flip, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
   }
}
