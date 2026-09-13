package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloudRenderer extends SimplePreparableReloadListener<Optional<CloudRenderer.TextureData>> implements AutoCloseable {
   private static final int FLAG_INSIDE_FACE = 16;
   private static final int FLAG_USE_TOP_COLOR = 32;
   private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
   private static final int TICKS_PER_CELL = 400;
   private static final float BLOCKS_PER_SECOND = 0.6F;
   private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/environment/clouds.png");
   private static final long EMPTY_CELL = 0L;
   private static final int COLOR_OFFSET = 4;
   private static final int NORTH_OFFSET = 3;
   private static final int EAST_OFFSET = 2;
   private static final int SOUTH_OFFSET = 1;
   private static final int WEST_OFFSET = 0;
   private boolean needsRebuild = true;
   private int prevCellX = Integer.MIN_VALUE;
   private int prevCellZ = Integer.MIN_VALUE;
   private CloudRenderer.RelativeCameraPos prevRelativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
   private @Nullable CloudStatus prevType;
   private CloudRenderer.@Nullable TextureData texture;
   private int quadCount = 0;
   private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
   private @Nullable MappableRingBuffer utb;

   protected Optional<CloudRenderer.TextureData> prepare(ResourceManager p_361257_, ProfilerFiller p_362196_) {
      try (
         InputStream inputstream = p_361257_.open(TEXTURE_LOCATION);
         NativeImage nativeimage = NativeImage.read(inputstream);
      ) {
         int i = nativeimage.getWidth();
         int j = nativeimage.getHeight();
         long[] along = new long[i * j];

         for (int k = 0; k < j; k++) {
            for (int l = 0; l < i; l++) {
               int i1 = nativeimage.getPixel(l, k);
               if (isCellEmpty(i1)) {
                  along[l + k * i] = 0L;
               } else {
                  boolean flag = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k - 1, j)));
                  boolean flag1 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l + 1, j), k));
                  boolean flag2 = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k + 1, j)));
                  boolean flag3 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l - 1, j), k));
                  along[l + k * i] = packCellData(i1, flag, flag1, flag2, flag3);
               }
            }
         }

         return Optional.of(new CloudRenderer.TextureData(along, i, j));
      } catch (IOException ioexception) {
         LOGGER.error("Failed to load cloud texture", ioexception);
         return Optional.empty();
      }
   }

   private static int getSizeForCloudDistance(int p_409968_) {
      int i = 4;
      int j = (p_409968_ + 1) * 2 * (p_409968_ + 1) * 2 / 2;
      int k = j * 4 + 54;
      return k * 3;
   }

   protected void apply(Optional<CloudRenderer.TextureData> p_370042_, ResourceManager p_368869_, ProfilerFiller p_367795_) {
      this.texture = p_370042_.orElse(null);
      this.needsRebuild = true;
   }

   private static boolean isCellEmpty(int p_366824_) {
      return ARGB.alpha(p_366824_) < 10;
   }

   private static long packCellData(int p_364599_, boolean p_362267_, boolean p_364671_, boolean p_363926_, boolean p_361986_) {
      return (long)p_364599_ << 4 | (p_362267_ ? 1 : 0) << 3 | (p_364671_ ? 1 : 0) << 2 | (p_363926_ ? 1 : 0) << 1 | (p_361986_ ? 1 : 0) << 0;
   }

   private static boolean isNorthEmpty(long p_369910_) {
      return (p_369910_ >> 3 & 1L) != 0L;
   }

   private static boolean isEastEmpty(long p_365859_) {
      return (p_365859_ >> 2 & 1L) != 0L;
   }

   private static boolean isSouthEmpty(long p_362752_) {
      return (p_362752_ >> 1 & 1L) != 0L;
   }

   private static boolean isWestEmpty(long p_366272_) {
      return (p_366272_ >> 0 & 1L) != 0L;
   }

   public void render(int p_369834_, CloudStatus p_363277_, float p_367079_, Vec3 p_367264_, long p_455582_, float p_364211_) {
      if (this.texture != null) {
         int i = Minecraft.getInstance().options.cloudRange().get() * 16;
         int j = Mth.ceil(i / 12.0F);
         int k = getSizeForCloudDistance(j);
         if (this.utb == null || this.utb.currentBuffer().size() != k) {
            if (this.utb != null) {
               this.utb.close();
            }

            this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, k);
         }

         float f = (float)(p_367079_ - p_367264_.y);
         float f1 = f + 4.0F;
         CloudRenderer.RelativeCameraPos cloudrenderer$relativecamerapos;
         if (f1 < 0.0F) {
            cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
         } else if (f > 0.0F) {
            cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
         } else {
            cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
         }

         float f2 = (float)(p_455582_ % (this.texture.width * 400L)) + p_364211_;
         double d0 = p_367264_.x + f2 * 0.030000001F;
         double d1 = p_367264_.z + 3.96F;
         double d2 = this.texture.width * 12.0;
         double d3 = this.texture.height * 12.0;
         d0 -= Mth.floor(d0 / d2) * d2;
         d1 -= Mth.floor(d1 / d3) * d3;
         int l = Mth.floor(d0 / 12.0);
         int i1 = Mth.floor(d1 / 12.0);
         float f3 = (float)(d0 - l * 12.0F);
         float f4 = (float)(d1 - i1 * 12.0F);
         boolean flag = p_363277_ == CloudStatus.FANCY;
         RenderPipeline renderpipeline = flag ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
         if (this.needsRebuild
            || l != this.prevCellX
            || i1 != this.prevCellZ
            || cloudrenderer$relativecamerapos != this.prevRelativeCameraPos
            || p_363277_ != this.prevType) {
            this.needsRebuild = false;
            this.prevCellX = l;
            this.prevCellZ = i1;
            this.prevRelativeCameraPos = cloudrenderer$relativecamerapos;
            this.prevType = p_363277_;
            this.utb.rotate();

            try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.utb.currentBuffer(), false, true)) {
               this.buildMesh(cloudrenderer$relativecamerapos, gpubuffer$mappedview.data(), l, i1, flag, j);
               this.quadCount = gpubuffer$mappedview.data().position() / 3;
            }
         }

         if (this.quadCount != 0) {
            try (GpuBuffer.MappedView gpubuffer$mappedview1 = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.ubo.currentBuffer(), false, true)) {
               Std140Builder.intoBuffer(gpubuffer$mappedview1.data())
                  .putVec4(ARGB.vector4fFromARGB32(p_369834_))
                  .putVec3(-f3, f, -f4)
                  .putVec3(12.0F, 4.0F, 12.0F);
            }

            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
               .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
            RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
            RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6 * this.quadCount);
            GpuTextureView gputextureview;
            GpuTextureView gputextureview1;
            if (rendertarget1 != null) {
               gputextureview = rendertarget1.getColorTextureView();
               gputextureview1 = rendertarget1.getDepthTextureView();
            } else {
               gputextureview = rendertarget.getColorTextureView();
               gputextureview1 = rendertarget.getDepthTextureView();
            }

            try (RenderPass renderpass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "Clouds", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
               renderpass.setPipeline(renderpipeline);
               RenderSystem.bindDefaultUniforms(renderpass);
               renderpass.setUniform("DynamicTransforms", gpubufferslice);
               renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
               renderpass.setUniform("CloudInfo", this.ubo.currentBuffer());
               renderpass.setUniform("CloudFaces", this.utb.currentBuffer());
               renderpass.drawIndexed(0, 0, 6 * this.quadCount, 1);
            }
         }
      }
   }

   private void buildMesh(CloudRenderer.RelativeCameraPos p_366327_, ByteBuffer p_409979_, int p_363487_, int p_363111_, boolean p_408259_, int p_407831_) {
      if (this.texture != null) {
         long[] along = this.texture.cells;
         int i = this.texture.width;
         int j = this.texture.height;

         for (int k = 0; k <= 2 * p_407831_; k++) {
            for (int l = -k; l <= k; l++) {
               int i1 = k - Math.abs(l);
               if (i1 >= 0 && i1 <= p_407831_ && l * l + i1 * i1 <= p_407831_ * p_407831_) {
                  if (i1 != 0) {
                     this.tryBuildCell(p_366327_, p_409979_, p_363487_, p_363111_, p_408259_, l, i, -i1, j, along);
                  }

                  this.tryBuildCell(p_366327_, p_409979_, p_363487_, p_363111_, p_408259_, l, i, i1, j, along);
               }
            }
         }
      }
   }

   private void tryBuildCell(
      CloudRenderer.RelativeCameraPos p_407461_,
      ByteBuffer p_408811_,
      int p_408333_,
      int p_410141_,
      boolean p_405870_,
      int p_409585_,
      int p_409250_,
      int p_409106_,
      int p_410230_,
      long[] p_409934_
   ) {
      int i = Math.floorMod(p_408333_ + p_409585_, p_409250_);
      int j = Math.floorMod(p_410141_ + p_409106_, p_410230_);
      long k = p_409934_[i + j * p_409250_];
      if (k != 0L) {
         if (p_405870_) {
            this.buildExtrudedCell(p_407461_, p_408811_, p_409585_, p_409106_, k);
         } else {
            this.buildFlatCell(p_408811_, p_409585_, p_409106_);
         }
      }
   }

   private void buildFlatCell(ByteBuffer p_408486_, int p_362314_, int p_368834_) {
      this.encodeFace(p_408486_, p_362314_, p_368834_, Direction.DOWN, 32);
   }

   private void encodeFace(ByteBuffer p_406958_, int p_409766_, int p_408056_, Direction p_407262_, int p_408144_) {
      int i = p_407262_.get3DDataValue() | p_408144_;
      i |= (p_409766_ & 1) << 7;
      i |= (p_408056_ & 1) << 6;
      p_406958_.put((byte)(p_409766_ >> 1)).put((byte)(p_408056_ >> 1)).put((byte)i);
   }

   private void buildExtrudedCell(CloudRenderer.RelativeCameraPos p_361197_, ByteBuffer p_410035_, int p_363655_, int p_363819_, long p_369137_) {
      if (p_361197_ != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
         this.encodeFace(p_410035_, p_363655_, p_363819_, Direction.UP, 0);
      }

      if (p_361197_ != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
         this.encodeFace(p_410035_, p_363655_, p_363819_, Direction.DOWN, 0);
      }

      if (isNorthEmpty(p_369137_) && p_363819_ > 0) {
         this.encodeFace(p_410035_, p_363655_, p_363819_, Direction.NORTH, 0);
      }

      if (isSouthEmpty(p_369137_) && p_363819_ < 0) {
         this.encodeFace(p_410035_, p_363655_, p_363819_, Direction.SOUTH, 0);
      }

      if (isWestEmpty(p_369137_) && p_363655_ > 0) {
         this.encodeFace(p_410035_, p_363655_, p_363819_, Direction.WEST, 0);
      }

      if (isEastEmpty(p_369137_) && p_363655_ < 0) {
         this.encodeFace(p_410035_, p_363655_, p_363819_, Direction.EAST, 0);
      }

      boolean flag = Math.abs(p_363655_) <= 1 && Math.abs(p_363819_) <= 1;
      if (flag) {
         for (Direction direction : Direction.values()) {
            this.encodeFace(p_410035_, p_363655_, p_363819_, direction, 16);
         }
      }
   }

   public void markForRebuild() {
      this.needsRebuild = true;
   }

   public void endFrame() {
      this.ubo.rotate();
   }

   @Override
   public void close() {
      this.ubo.close();
      if (this.utb != null) {
         this.utb.close();
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum RelativeCameraPos {
      ABOVE_CLOUDS,
      INSIDE_CLOUDS,
      BELOW_CLOUDS;
   }

   @OnlyIn(Dist.CLIENT)
   public record TextureData(long[] cells, int width, int height) {
   }
}
