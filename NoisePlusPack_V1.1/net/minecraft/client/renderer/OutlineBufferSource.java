package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OutlineBufferSource implements MultiBufferSource {
   private final MultiBufferSource.BufferSource outlineBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
   private int outlineColor = -1;

   @Override
   public VertexConsumer getBuffer(RenderType p_451538_) {
      if (p_451538_.isOutline()) {
         VertexConsumer vertexconsumer1 = this.outlineBufferSource.getBuffer(p_451538_);
         return new OutlineBufferSource.EntityOutlineGenerator(vertexconsumer1, this.outlineColor);
      } else {
         Optional<RenderType> optional = p_451538_.outline();
         if (optional.isPresent()) {
            VertexConsumer vertexconsumer = this.outlineBufferSource.getBuffer(optional.get());
            return new OutlineBufferSource.EntityOutlineGenerator(vertexconsumer, this.outlineColor);
         } else {
            throw new IllegalStateException("Can't render an outline for this rendertype!");
         }
      }
   }

   public void setColor(int p_109930_) {
      this.outlineColor = p_109930_;
   }

   public void endOutlineBatch() {
      this.outlineBufferSource.endBatch();
   }

   @OnlyIn(Dist.CLIENT)
   record EntityOutlineGenerator(VertexConsumer delegate, int color) implements VertexConsumer {
      @Override
      public VertexConsumer addVertex(float p_342958_, float p_343747_, float p_344781_) {
         this.delegate.addVertex(p_342958_, p_343747_, p_344781_).setColor(this.color);
         return this;
      }

      @Override
      public VertexConsumer setColor(int p_343483_, int p_343623_, int p_342060_, int p_342967_) {
         return this;
      }

      @Override
      public VertexConsumer setColor(int p_460149_) {
         return this;
      }

      @Override
      public VertexConsumer setUv(float p_342182_, float p_342633_) {
         this.delegate.setUv(p_342182_, p_342633_);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int p_344004_, int p_342637_) {
         return this;
      }

      @Override
      public VertexConsumer setUv2(int p_343797_, int p_342797_) {
         return this;
      }

      @Override
      public VertexConsumer setNormal(float p_343114_, float p_344978_, float p_343069_) {
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float p_455926_) {
         return this;
      }
   }
}
