package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class RenderType {
   private static final int MEGABYTE = 1048576;
   public static final int BIG_BUFFER_SIZE = 4194304;
   public static final int SMALL_BUFFER_SIZE = 786432;
   public static final int TRANSIENT_BUFFER_SIZE = 1536;
   private final RenderSetup state;
   private final Optional<RenderType> outline;
   protected final String name;

   private RenderType(String p_452093_, RenderSetup p_459167_) {
      this.name = p_452093_;
      this.state = p_459167_;
      this.outline = p_459167_.outlineProperty == RenderSetup.OutlineProperty.AFFECTS_OUTLINE
         ? p_459167_.textures.values().stream().findFirst().map(p_450841_ -> RenderTypes.OUTLINE.apply(p_450841_.location(), p_459167_.pipeline.isCull()))
         : Optional.empty();
   }

   static RenderType create(String p_452180_, RenderSetup p_453207_) {
      return new RenderType(p_452180_, p_453207_);
   }

   @Override
   public String toString() {
      return "RenderType[" + this.name + ":" + this.state + "]";
   }

   public void draw(MeshData p_458186_) {
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      Consumer<Matrix4fStack> consumer = this.state.layeringTransform.getModifier();
      if (consumer != null) {
         matrix4fstack.pushMatrix();
         consumer.accept(matrix4fstack);
      }

      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), this.state.textureTransform.getMatrix());
      Map<String, RenderSetup.TextureAndSampler> map = this.state.getTextures();
      MeshData meshdata = p_458186_;

      try {
         GpuBuffer gpubuffer = this.state.pipeline.getVertexFormat().uploadImmediateVertexBuffer(p_458186_.vertexBuffer());
         GpuBuffer gpubuffer1;
         VertexFormat.IndexType vertexformat$indextype;
         if (p_458186_.indexBuffer() == null) {
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(p_458186_.drawState().mode());
            gpubuffer1 = rendersystem$autostorageindexbuffer.getBuffer(p_458186_.drawState().indexCount());
            vertexformat$indextype = rendersystem$autostorageindexbuffer.type();
         } else {
            gpubuffer1 = this.state.pipeline.getVertexFormat().uploadImmediateIndexBuffer(p_458186_.indexBuffer());
            vertexformat$indextype = p_458186_.drawState().indexType();
         }

         RenderTarget rendertarget = this.state.outputTarget.getRenderTarget();
         GpuTextureView gputextureview = RenderSystem.outputColorTextureOverride != null
            ? RenderSystem.outputColorTextureOverride
            : rendertarget.getColorTextureView();
         GpuTextureView gputextureview1 = rendertarget.useDepth
            ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : rendertarget.getDepthTextureView())
            : null;

         try (RenderPass renderpass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Immediate draw for " + this.name, gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
            renderpass.setPipeline(this.state.pipeline);
            ScissorState scissorstate = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorstate.enabled()) {
               renderpass.enableScissor(scissorstate.x(), scissorstate.y(), scissorstate.width(), scissorstate.height());
            }

            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("DynamicTransforms", gpubufferslice);
            renderpass.setVertexBuffer(0, gpubuffer);

            for (Entry<String, RenderSetup.TextureAndSampler> entry : map.entrySet()) {
               renderpass.bindTexture(entry.getKey(), entry.getValue().textureView(), entry.getValue().sampler());
            }

            renderpass.setIndexBuffer(gpubuffer1, vertexformat$indextype);
            renderpass.drawIndexed(0, 0, p_458186_.drawState().indexCount(), 1);
         }
      } catch (Throwable throwable2) {
         if (p_458186_ != null) {
            try {
               meshdata.close();
            } catch (Throwable throwable) {
               throwable2.addSuppressed(throwable);
            }
         }

         throw throwable2;
      }

      if (p_458186_ != null) {
         p_458186_.close();
      }

      if (consumer != null) {
         matrix4fstack.popMatrix();
      }
   }

   public int bufferSize() {
      return this.state.bufferSize;
   }

   public VertexFormat format() {
      return this.state.pipeline.getVertexFormat();
   }

   public VertexFormat.Mode mode() {
      return this.state.pipeline.getVertexFormatMode();
   }

   public Optional<RenderType> outline() {
      return this.outline;
   }

   public boolean isOutline() {
      return this.state.outlineProperty == RenderSetup.OutlineProperty.IS_OUTLINE;
   }

   public RenderPipeline pipeline() {
      return this.state.pipeline;
   }

   public boolean affectsCrumbling() {
      return this.state.affectsCrumbling;
   }

   public boolean canConsolidateConsecutiveGeometry() {
      return !this.mode().connectedPrimitives;
   }

   public boolean sortOnUpload() {
      return this.state.sortOnUpload;
   }
}
