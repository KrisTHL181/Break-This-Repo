package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CompiledSectionMesh implements SectionMesh {
   public static final SectionMesh UNCOMPILED = new SectionMesh() {
      @Override
      public boolean facesCanSeeEachother(Direction p_407632_, Direction p_406501_) {
         return false;
      }
   };
   public static final SectionMesh EMPTY = new SectionMesh() {
      @Override
      public boolean facesCanSeeEachother(Direction p_406904_, Direction p_406261_) {
         return true;
      }
   };
   private final List<BlockEntity> renderableBlockEntities;
   private final VisibilitySet visibilitySet;
   private final MeshData.@Nullable SortState transparencyState;
   private @Nullable TranslucencyPointOfView translucencyPointOfView;
   private final Map<ChunkSectionLayer, SectionBuffers> buffers = new EnumMap<>(ChunkSectionLayer.class);

   public CompiledSectionMesh(TranslucencyPointOfView p_409073_, SectionCompiler.Results p_408572_) {
      this.translucencyPointOfView = p_409073_;
      this.visibilitySet = p_408572_.visibilitySet;
      this.renderableBlockEntities = p_408572_.blockEntities;
      this.transparencyState = p_408572_.transparencyState;
   }

   public void setTranslucencyPointOfView(TranslucencyPointOfView p_409288_) {
      this.translucencyPointOfView = p_409288_;
   }

   @Override
   public boolean isDifferentPointOfView(TranslucencyPointOfView p_407674_) {
      return !p_407674_.equals(this.translucencyPointOfView);
   }

   @Override
   public boolean hasRenderableLayers() {
      return !this.buffers.isEmpty();
   }

   @Override
   public boolean isEmpty(ChunkSectionLayer p_409252_) {
      return !this.buffers.containsKey(p_409252_);
   }

   @Override
   public List<BlockEntity> getRenderableBlockEntities() {
      return this.renderableBlockEntities;
   }

   @Override
   public boolean facesCanSeeEachother(Direction p_410263_, Direction p_407059_) {
      return this.visibilitySet.visibilityBetween(p_410263_, p_407059_);
   }

   @Override
   public @Nullable SectionBuffers getBuffers(ChunkSectionLayer p_409484_) {
      return this.buffers.get(p_409484_);
   }

   public void uploadMeshLayer(ChunkSectionLayer p_406382_, MeshData p_410167_, long p_406090_) {
      CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
      SectionBuffers sectionbuffers = this.getBuffers(p_406382_);
      if (sectionbuffers != null) {
         if (sectionbuffers.getVertexBuffer().size() < p_410167_.vertexBuffer().remaining()) {
            sectionbuffers.getVertexBuffer().close();
            sectionbuffers.setVertexBuffer(
               RenderSystem.getDevice()
                  .createBuffer(
                     () -> "Section vertex buffer - layer: "
                        + p_406382_.label()
                        + "; cords: "
                        + SectionPos.x(p_406090_)
                        + ", "
                        + SectionPos.y(p_406090_)
                        + ", "
                        + SectionPos.z(p_406090_),
                     40,
                     p_410167_.vertexBuffer()
                  )
            );
         } else if (!sectionbuffers.getVertexBuffer().isClosed()) {
            commandencoder.writeToBuffer(sectionbuffers.getVertexBuffer().slice(), p_410167_.vertexBuffer());
         }

         ByteBuffer bytebuffer = p_410167_.indexBuffer();
         if (bytebuffer != null) {
            if (sectionbuffers.getIndexBuffer() != null && sectionbuffers.getIndexBuffer().size() >= bytebuffer.remaining()) {
               if (!sectionbuffers.getIndexBuffer().isClosed()) {
                  commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), bytebuffer);
               }
            } else {
               if (sectionbuffers.getIndexBuffer() != null) {
                  sectionbuffers.getIndexBuffer().close();
               }

               sectionbuffers.setIndexBuffer(
                  RenderSystem.getDevice()
                     .createBuffer(
                        () -> "Section index buffer - layer: "
                           + p_406382_.label()
                           + "; cords: "
                           + SectionPos.x(p_406090_)
                           + ", "
                           + SectionPos.y(p_406090_)
                           + ", "
                           + SectionPos.z(p_406090_),
                        72,
                        bytebuffer
                     )
               );
            }
         } else if (sectionbuffers.getIndexBuffer() != null) {
            sectionbuffers.getIndexBuffer().close();
            sectionbuffers.setIndexBuffer(null);
         }

         sectionbuffers.setIndexCount(p_410167_.drawState().indexCount());
         sectionbuffers.setIndexType(p_410167_.drawState().indexType());
      } else {
         GpuBuffer gpubuffer1 = RenderSystem.getDevice()
            .createBuffer(
               () -> "Section vertex buffer - layer: "
                  + p_406382_.label()
                  + "; cords: "
                  + SectionPos.x(p_406090_)
                  + ", "
                  + SectionPos.y(p_406090_)
                  + ", "
                  + SectionPos.z(p_406090_),
               40,
               p_410167_.vertexBuffer()
            );
         ByteBuffer bytebuffer1 = p_410167_.indexBuffer();
         GpuBuffer gpubuffer = bytebuffer1 != null
            ? RenderSystem.getDevice()
               .createBuffer(
                  () -> "Section index buffer - layer: "
                     + p_406382_.label()
                     + "; cords: "
                     + SectionPos.x(p_406090_)
                     + ", "
                     + SectionPos.y(p_406090_)
                     + ", "
                     + SectionPos.z(p_406090_),
                  72,
                  bytebuffer1
               )
            : null;
         SectionBuffers sectionbuffers1 = new SectionBuffers(gpubuffer1, gpubuffer, p_410167_.drawState().indexCount(), p_410167_.drawState().indexType());
         this.buffers.put(p_406382_, sectionbuffers1);
      }
   }

   public void uploadLayerIndexBuffer(ChunkSectionLayer p_406407_, ByteBufferBuilder.Result p_409808_, long p_408270_) {
      SectionBuffers sectionbuffers = this.getBuffers(p_406407_);
      if (sectionbuffers != null) {
         if (sectionbuffers.getIndexBuffer() == null) {
            sectionbuffers.setIndexBuffer(
               RenderSystem.getDevice()
                  .createBuffer(
                     () -> "Section index buffer - layer: "
                        + p_406407_.label()
                        + "; cords: "
                        + SectionPos.x(p_408270_)
                        + ", "
                        + SectionPos.y(p_408270_)
                        + ", "
                        + SectionPos.z(p_408270_),
                     72,
                     p_409808_.byteBuffer()
                  )
            );
         } else {
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            if (!sectionbuffers.getIndexBuffer().isClosed()) {
               commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), p_409808_.byteBuffer());
            }
         }
      }
   }

   @Override
   public boolean hasTranslucentGeometry() {
      return this.buffers.containsKey(ChunkSectionLayer.TRANSLUCENT);
   }

   public MeshData.@Nullable SortState getTransparencyState() {
      return this.transparencyState;
   }

   @Override
   public void close() {
      this.buffers.values().forEach(SectionBuffers::close);
      this.buffers.clear();
   }
}
