package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.UvMapping;

public record SpriteCoordinateExpander(VertexConsumer delegate, UvMapping mapping) implements VertexConsumer {
   @Override
   public VertexConsumer addVertex(final float x, final float y, final float z) {
      this.delegate.addVertex(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setColor(final int r, final int g, final int b, final int a) {
      this.delegate.setColor(r, g, b, a);
      return this;
   }

   @Override
   public VertexConsumer setColor(final int color) {
      this.delegate.setColor(color);
      return this;
   }

   @Override
   public VertexConsumer setUv(final float u, final float v) {
      this.delegate.setUv(this.mapping.getU(u), this.mapping.getV(v));
      return this;
   }

   @Override
   public VertexConsumer setUv1(final int u, final int v) {
      this.delegate.setUv1(u, v);
      return this;
   }

   @Override
   public VertexConsumer setUv2(final int u, final int v) {
      this.delegate.setUv2(u, v);
      return this;
   }

   @Override
   public VertexConsumer setUv3(final float u, final float v) {
      return this.delegate.setUv3(u, v);
   }

   @Override
   public VertexConsumer setNormal(final float x, final float y, final float z) {
      this.delegate.setNormal(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setLineWidth(final float width) {
      this.delegate.setLineWidth(width);
      return this;
   }

   @Override
   public void addVertex(
      final float x,
      final float y,
      final float z,
      final int color,
      final float u,
      final float v,
      final int overlayCoords,
      final int lightCoords,
      final float nx,
      final float ny,
      final float nz
   ) {
      this.delegate.addVertex(x, y, z, color, this.mapping.getU(u), this.mapping.getV(v), overlayCoords, lightCoords, nx, ny, nz);
   }
}
