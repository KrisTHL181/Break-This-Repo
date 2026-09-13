package net.minecraft.client.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4fc;

public interface PlainTextRenderable extends TextRenderable.Styled {
   float DEFAULT_WIDTH = 8.0F;
   float DEFAULT_HEIGHT = 8.0F;
   float DEFUAULT_ASCENT = 8.0F;

   @Override
   default void render(final Matrix4fc pose, final VertexConsumer buffer, final int packedLightCoords, final boolean flat) {
      float frontDepth = 0.0F;
      if (this.shadowColor() != 0) {
         this.renderSprite(pose, buffer, packedLightCoords, this.shadowOffset(), this.shadowOffset(), 0.0F, this.shadowColor());
         if (!flat) {
            frontDepth += 0.03F;
         }
      }

      this.renderSprite(pose, buffer, packedLightCoords, 0.0F, 0.0F, frontDepth, this.color());
   }

   void renderSprite(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, float offsetX, float offsetY, float z, int color);

   float x();

   float y();

   int color();

   int shadowColor();

   float shadowOffset();

   default float width() {
      return 8.0F;
   }

   default float height() {
      return 8.0F;
   }

   default float ascent() {
      return 8.0F;
   }

   @Override
   default float left() {
      return this.x();
   }

   @Override
   default float right() {
      return this.left() + this.width();
   }

   @Override
   default float top() {
      return this.y() + 7.0F - this.ascent();
   }

   @Override
   default float bottom() {
      return this.activeTop() + this.height();
   }
}
