package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

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
   private @Nullable CloudStatus prevCloudStatus;
   private CloudRenderer.@Nullable TextureData texture;
   private int quadCount = 0;
   private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
   private @Nullable MappableRingBuffer utb;

   protected Optional<CloudRenderer.TextureData> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
      try (
         InputStream input = manager.open(TEXTURE_LOCATION);
         NativeImage texture = NativeImage.read(input);
      ) {
         int width = texture.getWidth();
         int height = texture.getHeight();
         long[] cells = new long[width * height];

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               int color = texture.getPixel(x, y);
               if (isCellEmpty(color)) {
                  cells[x + y * width] = 0L;
               } else {
                  boolean north = isCellEmpty(texture.getPixel(x, Math.floorMod(y - 1, height)));
                  boolean east = isCellEmpty(texture.getPixel(Math.floorMod(x + 1, height), y));
                  boolean south = isCellEmpty(texture.getPixel(x, Math.floorMod(y + 1, height)));
                  boolean west = isCellEmpty(texture.getPixel(Math.floorMod(x - 1, height), y));
                  cells[x + y * width] = packCellData(color, north, east, south, west);
               }
            }
         }

         return Optional.of(new CloudRenderer.TextureData(cells, width, height));
      } catch (IOException e) {
         LOGGER.error("Failed to load cloud texture", e);
         return Optional.empty();
      }
   }

   private static int getSizeForCloudDistance(final int radiusCells) {
      int maxFacesPerCell = 4;
      int maxCells = (radiusCells + 1) * 2 * (radiusCells + 1) * 2 / 2;
      int maxFaces = maxCells * 4 + 54;
      return maxFaces * 3;
   }

   protected void apply(final Optional<CloudRenderer.TextureData> preparations, final ResourceManager manager, final ProfilerFiller profiler) {
      this.texture = preparations.orElse(null);
      this.needsRebuild = true;
   }

   private static boolean isCellEmpty(final int color) {
      return ARGB.alpha(color) < 10;
   }

   private static long packCellData(final int color, final boolean north, final boolean east, final boolean south, final boolean west) {
      return (long)color << 4 | (north ? 1 : 0) << 3 | (east ? 1 : 0) << 2 | (south ? 1 : 0) << 1 | (west ? 1 : 0) << 0;
   }

   private static boolean isNorthEmpty(final long cellData) {
      return (cellData >> 3 & 1L) != 0L;
   }

   private static boolean isEastEmpty(final long cellData) {
      return (cellData >> 2 & 1L) != 0L;
   }

   private static boolean isSouthEmpty(final long cellData) {
      return (cellData >> 1 & 1L) != 0L;
   }

   private static boolean isWestEmpty(final long cellData) {
      return (cellData >> 0 & 1L) != 0L;
   }

   public void prepare(
      final int color,
      final CloudStatus cloudStatus,
      final float bottomY,
      final int range,
      final Vec3 cameraPosition,
      final long gameTime,
      final float partialTicks
   ) {
      if (this.texture != null) {
         int radiusBlocks = range * 16;
         int radiusCells = Mth.ceil(radiusBlocks / 12.0F);
         int utbSize = getSizeForCloudDistance(radiusCells);
         if (this.utb == null || this.utb.currentBuffer().size() != utbSize) {
            if (this.utb != null) {
               this.utb.close();
            }

            this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, utbSize);
         }

         float relativeBottomY = (float)(bottomY - cameraPosition.y);
         float relativeTopY = relativeBottomY + 4.0F;
         CloudRenderer.RelativeCameraPos relativeCameraPos;
         if (relativeTopY < 0.0F) {
            relativeCameraPos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
         } else if (relativeBottomY > 0.0F) {
            relativeCameraPos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
         } else {
            relativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
         }

         float cloudOffset = (float)(gameTime % (this.texture.width * 400L)) + partialTicks;
         double cloudX = cameraPosition.x + cloudOffset * 0.030000001F;
         double cloudZ = cameraPosition.z + 3.96F;
         double textureWidthBlocks = this.texture.width * 12.0;
         double textureHeightBlocks = this.texture.height * 12.0;
         cloudX -= Mth.floor(cloudX / textureWidthBlocks) * textureWidthBlocks;
         cloudZ -= Mth.floor(cloudZ / textureHeightBlocks) * textureHeightBlocks;
         int cellX = Mth.floor(cloudX / 12.0);
         int cellZ = Mth.floor(cloudZ / 12.0);
         float xInCell = (float)(cloudX - cellX * 12.0F);
         float zInCell = (float)(cloudZ - cellZ * 12.0F);
         boolean fancyClouds = cloudStatus == CloudStatus.FANCY;
         if (this.needsRebuild
            || cellX != this.prevCellX
            || cellZ != this.prevCellZ
            || relativeCameraPos != this.prevRelativeCameraPos
            || cloudStatus != this.prevCloudStatus) {
            this.needsRebuild = false;
            this.prevCellX = cellX;
            this.prevCellZ = cellZ;
            this.prevRelativeCameraPos = relativeCameraPos;
            this.prevCloudStatus = cloudStatus;
            this.utb.rotate();

            try (GpuBufferSlice.MappedView view = this.utb.currentBuffer().map(false, true)) {
               this.buildMesh(relativeCameraPos, view.data(), cellX, cellZ, fancyClouds, radiusCells);
               this.quadCount = view.data().position() / 3;
            }
         }

         if (this.quadCount != 0) {
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            indices.requestIndexCount(6 * this.quadCount);

            try (GpuBufferSlice.MappedView view = this.ubo.currentBuffer().map(false, true)) {
               Std140Builder.intoBuffer(view.data())
                  .putVec4(ARGB.vector4fFromARGB32(color))
                  .putVec3(-xInCell, relativeBottomY, -zInCell)
                  .putVec3(12.0F, 4.0F, 12.0F);
            }
         }
      }
   }

   public void render(final CloudStatus cloudStatus, final RenderPass renderPass) {
      if (this.texture != null && this.quadCount != 0) {
         RenderPipeline renderPipeline = cloudStatus == CloudStatus.FANCY ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
         renderPass.pushDebugGroup(() -> "Clouds");
         RenderSystem.bindDefaultUniforms(renderPass);
         this.render(renderPass, renderPipeline);
         renderPass.popDebugGroup();
      }
   }

   public void renderOit(final CloudStatus cloudStatus, final OitStage stage, final OitRenderPassProvider.Parameters params) {
      if (this.texture != null && this.quadCount != 0) {
         RenderPipeline renderPipeline = (cloudStatus == CloudStatus.FANCY ? RenderPipelines.OIT_CLOUDS : RenderPipelines.OIT_FLAT_CLOUDS).getPipeline(stage);

         try (RenderPass renderPass = OitRenderPassProvider.createRenderPass(stage, () -> "Clouds", params)) {
            this.render(renderPass, renderPipeline);
         }
      }
   }

   private void render(final RenderPass renderPass, final RenderPipeline renderPipeline) {
      GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
      RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
      GpuBuffer indexBuffer = indices.getBuffer();
      renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
      renderPass.setUniform("DynamicTransforms", dynamicTransforms);
      renderPass.setIndexBuffer(indexBuffer, indices.type());
      renderPass.setVertexBuffer(0, null);
      renderPass.setUniform("CloudInfo", this.ubo.currentBuffer());
      renderPass.setUniform("CloudFaces", this.utb.currentBuffer());
      renderPass.drawIndexed(6 * this.quadCount, 1, 0, 0, 0);
   }

   private void buildMesh(
      final CloudRenderer.RelativeCameraPos relativePos,
      final ByteBuffer faceBuffer,
      final int centerCellX,
      final int centerCellZ,
      final boolean extrude,
      final int radiusCells
   ) {
      if (this.texture != null) {
         long[] cells = this.texture.cells;
         int textureWidth = this.texture.width;
         int textureHeight = this.texture.height;

         for (int ring = 0; ring <= 2 * radiusCells; ring++) {
            for (int relativeCellX = -ring; relativeCellX <= ring; relativeCellX++) {
               int relativeCellZ = ring - Math.abs(relativeCellX);
               if (relativeCellZ >= 0
                  && relativeCellZ <= radiusCells
                  && relativeCellX * relativeCellX + relativeCellZ * relativeCellZ <= radiusCells * radiusCells) {
                  if (relativeCellZ != 0) {
                     this.tryBuildCell(
                        relativePos, faceBuffer, centerCellX, centerCellZ, extrude, relativeCellX, textureWidth, -relativeCellZ, textureHeight, cells
                     );
                  }

                  this.tryBuildCell(
                     relativePos, faceBuffer, centerCellX, centerCellZ, extrude, relativeCellX, textureWidth, relativeCellZ, textureHeight, cells
                  );
               }
            }
         }
      }
   }

   private void tryBuildCell(
      final CloudRenderer.RelativeCameraPos relativePos,
      final ByteBuffer faceBuffer,
      final int cellX,
      final int cellZ,
      final boolean extrude,
      final int relativeCellX,
      final int textureWidth,
      final int relativeCellZ,
      final int textureHeight,
      final long[] cells
   ) {
      int indexX = Math.floorMod(cellX + relativeCellX, textureWidth);
      int indexY = Math.floorMod(cellZ + relativeCellZ, textureHeight);
      long cellData = cells[indexX + indexY * textureWidth];
      if (cellData != 0L) {
         if (extrude) {
            this.buildExtrudedCell(relativePos, faceBuffer, relativeCellX, relativeCellZ, cellData);
         } else {
            this.buildFlatCell(faceBuffer, relativeCellX, relativeCellZ);
         }
      }
   }

   private void buildFlatCell(final ByteBuffer faceBuffer, final int x, final int z) {
      this.encodeFace(faceBuffer, x, z, Direction.DOWN, 32);
   }

   private void encodeFace(final ByteBuffer faceBuffer, final int x, final int z, final Direction direction, final int flags) {
      int dirAndFlags = direction.get3DDataValue() | flags;
      dirAndFlags |= (x & 1) << 7;
      dirAndFlags |= (z & 1) << 6;
      faceBuffer.put((byte)(x >> 1)).put((byte)(z >> 1)).put((byte)dirAndFlags);
   }

   private void buildExtrudedCell(final CloudRenderer.RelativeCameraPos relativePos, final ByteBuffer faceBuffer, final int x, final int z, final long cellData) {
      if (relativePos != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
         this.encodeFace(faceBuffer, x, z, Direction.UP, 0);
      }

      if (relativePos != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
         this.encodeFace(faceBuffer, x, z, Direction.DOWN, 0);
      }

      if (isNorthEmpty(cellData) && z > 0) {
         this.encodeFace(faceBuffer, x, z, Direction.NORTH, 0);
      }

      if (isSouthEmpty(cellData) && z < 0) {
         this.encodeFace(faceBuffer, x, z, Direction.SOUTH, 0);
      }

      if (isWestEmpty(cellData) && x > 0) {
         this.encodeFace(faceBuffer, x, z, Direction.WEST, 0);
      }

      if (isEastEmpty(cellData) && x < 0) {
         this.encodeFace(faceBuffer, x, z, Direction.EAST, 0);
      }

      boolean addInteriorFaces = Math.abs(x) <= 1 && Math.abs(z) <= 1;
      if (addInteriorFaces) {
         for (Direction direction : Direction.values()) {
            this.encodeFace(faceBuffer, x, z, direction, 16);
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

   private enum RelativeCameraPos {
      ABOVE_CLOUDS,
      INSIDE_CLOUDS,
      BELOW_CLOUDS;
   }

   public record TextureData(long[] cells, int width, int height) {
   }
}
