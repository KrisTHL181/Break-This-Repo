package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

@OnlyIn(Dist.CLIENT)
public class DynamicUniforms implements AutoCloseable {
   public static final int TRANSFORM_UBO_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec3().putMat4f().get();
   public static final int CHUNK_SECTION_UBO_SIZE = new Std140SizeCalculator().putMat4f().putFloat().putIVec2().putIVec3().get();
   private static final int INITIAL_CAPACITY = 2;
   private final DynamicUniformStorage<DynamicUniforms.Transform> transforms = new DynamicUniformStorage<>("Dynamic Transforms UBO", TRANSFORM_UBO_SIZE, 2);
   private final DynamicUniformStorage<DynamicUniforms.ChunkSectionInfo> chunkSections = new DynamicUniformStorage<>(
      "Chunk Sections UBO", CHUNK_SECTION_UBO_SIZE, 2
   );

   public void reset() {
      this.transforms.endFrame();
      this.chunkSections.endFrame();
   }

   @Override
   public void close() {
      this.transforms.close();
      this.chunkSections.close();
   }

   public GpuBufferSlice writeTransform(Matrix4fc p_408013_, Vector4fc p_409906_, Vector3fc p_408651_, Matrix4fc p_406035_) {
      return this.transforms
         .writeUniform(new DynamicUniforms.Transform(new Matrix4f(p_408013_), new Vector4f(p_409906_), new Vector3f(p_408651_), new Matrix4f(p_406035_)));
   }

   public GpuBufferSlice[] writeTransforms(DynamicUniforms.Transform... p_409797_) {
      return this.transforms.writeUniforms(p_409797_);
   }

   public GpuBufferSlice[] writeChunkSections(DynamicUniforms.ChunkSectionInfo... p_450990_) {
      return this.chunkSections.writeUniforms(p_450990_);
   }

   @OnlyIn(Dist.CLIENT)
   public record ChunkSectionInfo(Matrix4fc modelView, int x, int y, int z, float visibility, int textureAtlasWidth, int textureAtlasHeight)
      implements DynamicUniformStorage.DynamicUniform {
      @Override
      public void write(ByteBuffer p_456553_) {
         Std140Builder.intoBuffer(p_456553_)
            .putMat4f(this.modelView)
            .putFloat(this.visibility)
            .putIVec2(this.textureAtlasWidth, this.textureAtlasHeight)
            .putIVec3(this.x, this.y, this.z);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Transform(Matrix4fc modelView, Vector4fc colorModulator, Vector3fc modelOffset, Matrix4fc textureMatrix)
      implements DynamicUniformStorage.DynamicUniform {
      @Override
      public void write(ByteBuffer p_408538_) {
         Std140Builder.intoBuffer(p_408538_).putMat4f(this.modelView).putVec4(this.colorModulator).putVec3(this.modelOffset).putMat4f(this.textureMatrix);
      }
   }
}
