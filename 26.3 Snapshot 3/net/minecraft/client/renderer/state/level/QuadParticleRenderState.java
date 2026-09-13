package net.minecraft.client.renderer.state.level;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class QuadParticleRenderState implements ParticleGroupRenderState {
   private static final int INITIAL_PARTICLE_CAPACITY = 1024;
   private static final int FLOATS_PER_PARTICLE = 12;
   private static final int INTS_PER_PARTICLE = 2;
   private final Map<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> particles = new HashMap<>();
   private int particleCount;

   public void add(
      final SingleQuadParticle.Layer layer,
      final float x,
      final float y,
      final float z,
      final float xRot,
      final float yRot,
      final float zRot,
      final float wRot,
      final float scale,
      final float u0,
      final float u1,
      final float v0,
      final float v1,
      final int color,
      final int lightCoords
   ) {
      this.particles
         .computeIfAbsent(layer, ignored -> new QuadParticleRenderState.Storage())
         .add(x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords);
      this.particleCount++;
   }

   @Override
   public void clear() {
      this.particles.values().forEach(QuadParticleRenderState.Storage::clear);
      this.particleCount = 0;
   }

   public boolean isEmpty() {
      return this.particleCount == 0;
   }

   public void buildLayer(final SingleQuadParticle.Layer layer, final VertexConsumer bufferBuilder) {
      QuadParticleRenderState.Storage storage = this.particles.get(layer);
      if (storage != null) {
         storage.forEachParticle(
            (x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords) -> this.renderRotatedQuad(
               bufferBuilder, x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords
            )
         );
      }
   }

   public Set<SingleQuadParticle.Layer> layers() {
      return this.particles.keySet();
   }

   protected void renderRotatedQuad(
      final VertexConsumer builder,
      final float x,
      final float y,
      final float z,
      final float xRot,
      final float yRot,
      final float zRot,
      final float wRot,
      final float scale,
      final float u0,
      final float u1,
      final float v0,
      final float v1,
      final int color,
      final int lightCoords
   ) {
      Quaternionf rotation = new Quaternionf(xRot, yRot, zRot, wRot);
      this.renderVertex(builder, rotation, x, y, z, 1.0F, -1.0F, scale, u1, v1, color, lightCoords);
      this.renderVertex(builder, rotation, x, y, z, 1.0F, 1.0F, scale, u1, v0, color, lightCoords);
      this.renderVertex(builder, rotation, x, y, z, -1.0F, 1.0F, scale, u0, v0, color, lightCoords);
      this.renderVertex(builder, rotation, x, y, z, -1.0F, -1.0F, scale, u0, v1, color, lightCoords);
   }

   private void renderVertex(
      final VertexConsumer builder,
      final Quaternionf rotation,
      final float x,
      final float y,
      final float z,
      final float nx,
      final float ny,
      final float scale,
      final float u,
      final float v,
      final int color,
      final int lightCoords
   ) {
      Vector3f scratch = new Vector3f(nx, ny, 0.0F).rotate(rotation).mul(scale).add(x, y, z);
      builder.addVertex(scratch.x(), scratch.y(), scratch.z()).setUv(u, v).setColor(color).setLight(lightCoords);
   }

   @Override
   public void submit(final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (this.particleCount > 0) {
         submitNodeCollector.submitQuadParticleGroup(this);
      }
   }

   @FunctionalInterface
   public interface ParticleConsumer {
      void consume(
         final float x,
         final float y,
         final float z,
         final float xRot,
         final float yRot,
         final float zRot,
         final float wRot,
         final float scale,
         final float u0,
         final float u1,
         final float v0,
         final float v1,
         final int color,
         final int lightCoords
      );
   }

   private static class Storage {
      private int capacity = 1024;
      private float[] floatValues = new float[12288];
      private int[] intValues = new int[2048];
      private int currentParticleIndex;

      public void add(
         final float x,
         final float y,
         final float z,
         final float xRot,
         final float yRot,
         final float zRot,
         final float wRot,
         final float scale,
         final float u0,
         final float u1,
         final float v0,
         final float v1,
         final int color,
         final int lightCoords
      ) {
         if (this.currentParticleIndex >= this.capacity) {
            this.grow();
         }

         int index = this.currentParticleIndex * 12;
         this.floatValues[index++] = x;
         this.floatValues[index++] = y;
         this.floatValues[index++] = z;
         this.floatValues[index++] = xRot;
         this.floatValues[index++] = yRot;
         this.floatValues[index++] = zRot;
         this.floatValues[index++] = wRot;
         this.floatValues[index++] = scale;
         this.floatValues[index++] = u0;
         this.floatValues[index++] = u1;
         this.floatValues[index++] = v0;
         this.floatValues[index] = v1;
         index = this.currentParticleIndex * 2;
         this.intValues[index++] = color;
         this.intValues[index] = lightCoords;
         this.currentParticleIndex++;
      }

      public void forEachParticle(final QuadParticleRenderState.ParticleConsumer consumer) {
         for (int particleIndex = 0; particleIndex < this.currentParticleIndex; particleIndex++) {
            int floatIndex = particleIndex * 12;
            int intIndex = particleIndex * 2;
            consumer.consume(
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex++],
               this.floatValues[floatIndex],
               this.intValues[intIndex++],
               this.intValues[intIndex]
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
