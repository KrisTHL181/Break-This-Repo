package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;

public interface UvMapping {
   float getU(float offset);

   float getV(float offset);

   default VertexConsumer wrap(final VertexConsumer buffer) {
      return new SpriteCoordinateExpander(buffer, this);
   }
}
