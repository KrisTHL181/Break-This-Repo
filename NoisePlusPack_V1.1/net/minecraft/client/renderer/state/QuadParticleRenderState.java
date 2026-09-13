package net.minecraft.client.renderer.state;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class QuadParticleRenderState implements SubmitNodeCollector.ParticleGroupRenderer, ParticleGroupRenderState {
   private static final int INITIAL_PARTICLE_CAPACITY = 1024;
   private static final int FLOATS_PER_PARTICLE = 12;
   private static final int INTS_PER_PARTICLE = 2;
   private final Map<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> particles = new HashMap<>();
   private int particleCount;

   public void add(
      SingleQuadParticle.Layer p_423320_,
      float p_425334_,
      float p_428174_,
      float p_429572_,
      float p_430629_,
      float p_428024_,
      float p_430210_,
      float p_423284_,
      float p_428012_,
      float p_430111_,
      float p_430584_,
      float p_424344_,
      float p_422739_,
      int p_430341_,
      int p_425088_
   ) {
      this.particles
         .computeIfAbsent(p_423320_, p_425620_ -> new QuadParticleRenderState.Storage())
         .add(
            p_425334_,
            p_428174_,
            p_429572_,
            p_430629_,
            p_428024_,
            p_430210_,
            p_423284_,
            p_428012_,
            p_430111_,
            p_430584_,
            p_424344_,
            p_422739_,
            p_430341_,
            p_425088_
         );
      this.particleCount++;
   }

   @Override
   public void clear() {
      this.particles.values().forEach(QuadParticleRenderState.Storage::clear);
      this.particleCount = 0;
   }

   @Override
   public QuadParticleRenderState.@Nullable PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache p_429684_) {
      int i = this.particleCount * 4;

      Object object;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(i * DefaultVertexFormat.PARTICLE.getVertexSize())) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
         Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> map = new HashMap<>();
         int j = 0;

         for (Entry<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> entry : this.particles.entrySet()) {
            entry.getValue()
               .forEachParticle(
                  (p_427804_, p_426935_, p_430314_, p_427886_, p_424206_, p_430763_, p_424216_, p_425782_, p_430493_, p_423446_, p_424234_, p_424524_, p_423490_, p_429449_) -> this.renderRotatedQuad(
                     bufferbuilder,
                     p_427804_,
                     p_426935_,
                     p_430314_,
                     p_427886_,
                     p_424206_,
                     p_430763_,
                     p_424216_,
                     p_425782_,
                     p_430493_,
                     p_423446_,
                     p_424234_,
                     p_424524_,
                     p_423490_,
                     p_429449_
                  )
               );
            if (entry.getValue().count() > 0) {
               map.put(entry.getKey(), new QuadParticleRenderState.PreparedLayer(j, entry.getValue().count() * 6));
            }

            j += entry.getValue().count() * 4;
         }

         MeshData meshdata = bufferbuilder.build();
         if (meshdata != null) {
            p_429684_.write(meshdata.vertexBuffer());
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(meshdata.drawState().indexCount());
            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
               .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            return new QuadParticleRenderState.PreparedBuffers(meshdata.drawState().indexCount(), gpubufferslice, map);
         }

         object = null;
      }

      return (QuadParticleRenderState.PreparedBuffers)object;
   }

   @Override
   public void render(
      QuadParticleRenderState.PreparedBuffers p_425629_,
      ParticleFeatureRenderer.ParticleBufferCache p_430205_,
      RenderPass p_430224_,
      TextureManager p_431291_,
      boolean p_424393_
   ) {
      RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      p_430224_.setVertexBuffer(0, p_430205_.get());
      p_430224_.setIndexBuffer(rendersystem$autostorageindexbuffer.getBuffer(p_425629_.indexCount), rendersystem$autostorageindexbuffer.type());
      p_430224_.setUniform("DynamicTransforms", p_425629_.dynamicTransforms);

      for (Entry<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> entry : p_425629_.layers.entrySet()) {
         if (p_424393_ == entry.getKey().translucent()) {
            p_430224_.setPipeline(entry.getKey().pipeline());
            AbstractTexture abstracttexture = p_431291_.getTexture(entry.getKey().textureAtlasLocation());
            p_430224_.bindTexture("Sampler0", abstracttexture.getTextureView(), abstracttexture.getSampler());
            p_430224_.drawIndexed(entry.getValue().vertexOffset, 0, entry.getValue().indexCount, 1);
         }
      }
   }

   protected void renderRotatedQuad(
      VertexConsumer p_427139_,
      float p_425011_,
      float p_422470_,
      float p_428435_,
      float p_426314_,
      float p_422606_,
      float p_428492_,
      float p_422649_,
      float p_426421_,
      float p_431060_,
      float p_424479_,
      float p_426528_,
      float p_422524_,
      int p_428774_,
      int p_431539_
   ) {
      Quaternionf quaternionf = new Quaternionf(p_426314_, p_422606_, p_428492_, p_422649_);
      this.renderVertex(p_427139_, quaternionf, p_425011_, p_422470_, p_428435_, 1.0F, -1.0F, p_426421_, p_424479_, p_422524_, p_428774_, p_431539_);
      this.renderVertex(p_427139_, quaternionf, p_425011_, p_422470_, p_428435_, 1.0F, 1.0F, p_426421_, p_424479_, p_426528_, p_428774_, p_431539_);
      this.renderVertex(p_427139_, quaternionf, p_425011_, p_422470_, p_428435_, -1.0F, 1.0F, p_426421_, p_431060_, p_426528_, p_428774_, p_431539_);
      this.renderVertex(p_427139_, quaternionf, p_425011_, p_422470_, p_428435_, -1.0F, -1.0F, p_426421_, p_431060_, p_422524_, p_428774_, p_431539_);
   }

   private void renderVertex(
      VertexConsumer p_431441_,
      Quaternionf p_422638_,
      float p_430496_,
      float p_422414_,
      float p_424265_,
      float p_428184_,
      float p_431345_,
      float p_428900_,
      float p_429925_,
      float p_429815_,
      int p_427364_,
      int p_422567_
   ) {
      Vector3f vector3f = new Vector3f(p_428184_, p_431345_, 0.0F).rotate(p_422638_).mul(p_428900_).add(p_430496_, p_422414_, p_424265_);
      p_431441_.addVertex(vector3f.x(), vector3f.y(), vector3f.z()).setUv(p_429925_, p_429815_).setColor(p_427364_).setLight(p_422567_);
   }

   @Override
   public void submit(SubmitNodeCollector p_430906_, CameraRenderState p_423941_) {
      if (this.particleCount > 0) {
         p_430906_.submitParticleGroup(this);
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ParticleConsumer {
      void consume(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         float var11,
         float var12,
         int var13,
         int var14
      );
   }

   @OnlyIn(Dist.CLIENT)
   public record PreparedBuffers(int indexCount, GpuBufferSlice dynamicTransforms, Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> layers) {
   }

   @OnlyIn(Dist.CLIENT)
   public record PreparedLayer(int vertexOffset, int indexCount) {
   }

   @OnlyIn(Dist.CLIENT)
   static class Storage {
      private int capacity = 1024;
      private float[] floatValues = new float[12288];
      private int[] intValues = new int[2048];
      private int currentParticleIndex;

      public void add(
         float p_430868_,
         float p_425658_,
         float p_429857_,
         float p_429079_,
         float p_422440_,
         float p_430473_,
         float p_423904_,
         float p_428068_,
         float p_427815_,
         float p_430660_,
         float p_423096_,
         float p_430333_,
         int p_427052_,
         int p_425233_
      ) {
         if (this.currentParticleIndex >= this.capacity) {
            this.grow();
         }

         int i = this.currentParticleIndex * 12;
         this.floatValues[i++] = p_430868_;
         this.floatValues[i++] = p_425658_;
         this.floatValues[i++] = p_429857_;
         this.floatValues[i++] = p_429079_;
         this.floatValues[i++] = p_422440_;
         this.floatValues[i++] = p_430473_;
         this.floatValues[i++] = p_423904_;
         this.floatValues[i++] = p_428068_;
         this.floatValues[i++] = p_427815_;
         this.floatValues[i++] = p_430660_;
         this.floatValues[i++] = p_423096_;
         this.floatValues[i] = p_430333_;
         i = this.currentParticleIndex * 2;
         this.intValues[i++] = p_427052_;
         this.intValues[i] = p_425233_;
         this.currentParticleIndex++;
      }

      public void forEachParticle(QuadParticleRenderState.ParticleConsumer p_428883_) {
         for (int i = 0; i < this.currentParticleIndex; i++) {
            int j = i * 12;
            int k = i * 2;
            p_428883_.consume(
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j++],
               this.floatValues[j],
               this.intValues[k++],
               this.intValues[k]
            );
         }
      }

      public void clear() {
         this.currentParticleIndex = 0;
      }

      private void grow() {
         this.capacity *= 2;
         this.floatValues = Arrays.copyOf(this.floatValues, this.capacity * 12);
         this.intValues = Arrays.copyOf(this.intValues, this.capacity * 2);
      }

      public int count() {
         return this.currentParticleIndex;
      }
   }
}
