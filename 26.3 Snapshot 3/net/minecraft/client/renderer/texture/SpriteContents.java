package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SpriteContents implements AutoCloseable, Stitcher.Entry {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int UBO_SIZE = new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();
   private final Identifier name;
   private final int width;
   private final int height;
   private final NativeImage originalImage;
   private NativeImage[] byMipLevel;
   private final SpriteContents.@Nullable AnimatedTexture animatedTexture;
   private final List<MetadataSectionType.WithValue<?>> additionalMetadata;
   private final MipmapStrategy mipmapStrategy;
   private final float alphaCutoffBias;
   private final Transparency transparency;

   public SpriteContents(final Identifier name, final FrameSize frameSize, final NativeImage image) {
      this(name, frameSize, image, Optional.empty(), List.of(), Optional.empty());
   }

   public SpriteContents(
      final Identifier name,
      final FrameSize frameSize,
      final NativeImage image,
      final Optional<AnimationMetadataSection> animationInfo,
      final List<MetadataSectionType.WithValue<?>> additionalMetadata,
      final Optional<TextureMetadataSection> textureInfo
   ) {
      this.name = name;
      this.width = frameSize.width();
      this.height = frameSize.height();
      this.additionalMetadata = additionalMetadata;
      this.animatedTexture = animationInfo.<SpriteContents.AnimatedTexture>map(
            animation -> this.createAnimatedTexture(frameSize, image.getWidth(), image.getHeight(), animation)
         )
         .orElse(null);
      this.originalImage = image;
      this.byMipLevel = new NativeImage[]{this.originalImage};
      this.mipmapStrategy = textureInfo.map(TextureMetadataSection::mipmapStrategy).orElse(MipmapStrategy.AUTO);
      this.alphaCutoffBias = textureInfo.map(TextureMetadataSection::alphaCutoffBias).orElse(0.0F);
      this.transparency = image.computeTransparency();
   }

   public void increaseMipLevel(final int mipLevel) {
      try {
         this.byMipLevel = MipmapGenerator.generateMipLevels(this.name, this.byMipLevel, mipLevel, this.mipmapStrategy, this.alphaCutoffBias, this.transparency);
      } catch (Throwable t) {
         CrashReport report = CrashReport.forThrowable(t, "Generating mipmaps for frame");
         CrashReportCategory frameCategory = report.addCategory("Frame being iterated");
         frameCategory.setDetail("Sprite name", this.name);
         frameCategory.setDetail("Sprite size", () -> this.width + " x " + this.height);
         frameCategory.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
         frameCategory.setDetail("Mipmap levels", mipLevel);
         frameCategory.setDetail("Original image size", () -> this.originalImage.getWidth() + "x" + this.originalImage.getHeight());
         throw new ReportedException(report);
      }
   }

   private int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   public boolean isAnimated() {
      return this.getFrameCount() > 1;
   }

   public Transparency transparency() {
      return this.transparency;
   }

   private SpriteContents.@Nullable AnimatedTexture createAnimatedTexture(
      final FrameSize frameSize, final int fullWidth, final int fullHeight, final AnimationMetadataSection metadata
   ) {
      int frameRowSize = fullWidth / frameSize.width();
      int frameColumnSize = fullHeight / frameSize.height();
      int totalFrameCount = frameRowSize * frameColumnSize;
      int defaultFrameTime = metadata.defaultFrameTime();
      List<SpriteContents.FrameInfo> frames;
      if (metadata.frames().isEmpty()) {
         frames = new ArrayList<>(totalFrameCount);

         for (int i = 0; i < totalFrameCount; i++) {
            frames.add(new SpriteContents.FrameInfo(i, defaultFrameTime));
         }
      } else {
         List<AnimationFrame> metadataFrames = metadata.frames().get();
         frames = new ArrayList<>(metadataFrames.size());

         for (AnimationFrame frame : metadataFrames) {
            frames.add(new SpriteContents.FrameInfo(frame.index(), frame.timeOr(defaultFrameTime)));
         }

         int index = 0;
         IntSet usedFrameIndices = new IntOpenHashSet();

         for (Iterator<SpriteContents.FrameInfo> iterator = frames.iterator(); iterator.hasNext(); index++) {
            SpriteContents.FrameInfo frame = iterator.next();
            boolean isValid = true;
            if (frame.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.name, index, frame.time});
               isValid = false;
            }

            if (frame.index < 0 || frame.index >= totalFrameCount) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.name, index, frame.index});
               isValid = false;
            }

            if (isValid) {
               usedFrameIndices.add(frame.index);
            } else {
               iterator.remove();
            }
         }

         int[] unusedFrameIndices = IntStream.range(0, totalFrameCount).filter(i -> !usedFrameIndices.contains(i)).toArray();
         if (unusedFrameIndices.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(unusedFrameIndices));
         }
      }

      return frames.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(frames), frameRowSize, metadata.interpolatedFrames());
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   @Override
   public Identifier name() {
      return this.name;
   }

   public IntList getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntList.of(1);
   }

   public SpriteContents.@Nullable AnimationState createAnimationState(final GpuBufferSlice uboSlice, final int spriteUboSize) {
      return this.animatedTexture != null ? this.animatedTexture.createAnimationState(uboSlice, spriteUboSize) : null;
   }

   public <T> Optional<T> getAdditionalMetadata(final MetadataSectionType<T> type) {
      for (MetadataSectionType.WithValue<?> metadata : this.additionalMetadata) {
         Optional<T> result = metadata.unwrapToType(type);
         if (result.isPresent()) {
            return result;
         }
      }

      return Optional.empty();
   }

   @Override
   public void close() {
      for (NativeImage image : this.byMipLevel) {
         image.close();
      }
   }

   @Override
   public String toString() {
      return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isTransparent(final int frame, final int x, final int y) {
      int actualX = x;
      int actualY = y;
      if (this.animatedTexture != null) {
         actualX += this.animatedTexture.getFrameX(frame) * this.width;
         actualY += this.animatedTexture.getFrameY(frame) * this.height;
      }

      return ARGB.alpha(this.originalImage.getPixel(actualX, actualY)) == 0;
   }

   public Transparency computeTransparency(final float u0, final float v0, final float u1, final float v1) {
      if (this.transparency.isOpaque()) {
         return this.transparency;
      }

      if (u0 == 0.0F && v0 == 0.0F && u1 == 1.0F && v1 == 1.0F) {
         return this.transparency;
      }

      int x0 = Mth.floor(u0 * this.width);
      int y0 = Mth.floor(v0 * this.height);
      int x1 = Mth.ceil(u1 * this.width);
      int y1 = Mth.ceil(v1 * this.height);
      if (this.animatedTexture == null) {
         return this.originalImage.computeTransparency(x0, y0, x1, y1);
      }

      IntList uniqueFrames = this.animatedTexture.uniqueFrames;
      Transparency transparency = Transparency.NONE;

      for (int i = 0; i < uniqueFrames.size(); i++) {
         int frame = uniqueFrames.getInt(i);
         int frameX = this.animatedTexture.getFrameX(frame) * this.width;
         int frameY = this.animatedTexture.getFrameY(frame) * this.height;
         transparency = transparency.or(this.originalImage.computeTransparency(frameX + x0, frameY + y0, frameX + x1, frameY + y1));
      }

      return transparency;
   }

   public void uploadFirstFrame(final GpuTexture destination, final int level) {
      RenderSystem.getDevice().createCommandEncoder().writeToTexture(destination, this.byMipLevel[level], level, 0, 0, 0);
   }

   private class AnimatedTexture {
      private final List<SpriteContents.FrameInfo> frames;
      private final IntList uniqueFrames;
      private final int frameRowSize;
      private final boolean interpolateFrames;

      private AnimatedTexture(final List<SpriteContents.FrameInfo> frames, final int frameRowSize, final boolean interpolateFrames) {
         this.frames = frames;
         this.frameRowSize = frameRowSize;
         this.interpolateFrames = interpolateFrames;
         this.uniqueFrames = IntArrayList.toList(frames.stream().mapToInt(SpriteContents.FrameInfo::index).distinct());
      }

      private int getFrameX(final int index) {
         return index % this.frameRowSize;
      }

      private int getFrameY(final int index) {
         return index / this.frameRowSize;
      }

      public SpriteContents.AnimationState createAnimationState(final GpuBufferSlice uboSlice, final int spriteUboSize) {
         GpuDevice device = RenderSystem.getDevice();
         Int2ObjectMap<GpuTextureView> frameTexturesByIndex = new Int2ObjectOpenHashMap();
         GpuBufferSlice[] spriteUbosByMip = new GpuBufferSlice[SpriteContents.this.byMipLevel.length];
         CommandEncoder encoder = device.createCommandEncoder();
         List<GpuBufferSlice> stagingBuffers = encoder.transientMemory()
            .multiUploadStaging(Arrays.stream(SpriteContents.this.byMipLevel).map(NativeImage::getPixelBytes).toList(), 1L, 16);

         for (int i = 0; i < this.uniqueFrames.size(); i++) {
            int frame = this.uniqueFrames.getInt(i);
            GpuTexture texture = device.createTexture(
               () -> SpriteContents.this.name + " animation frame " + frame,
               5,
               GpuFormat.RGBA8_UNORM,
               SpriteContents.this.width,
               SpriteContents.this.height,
               1,
               SpriteContents.this.byMipLevel.length
            );
            int offsetX = this.getFrameX(frame) * SpriteContents.this.width;
            int offsetY = this.getFrameY(frame) * SpriteContents.this.height;

            for (int level = 0; level < SpriteContents.this.byMipLevel.length; level++) {
               encoder.copyBufferToTexture(
                  stagingBuffers.get(level),
                  offsetX >> level,
                  offsetY >> level,
                  SpriteContents.this.byMipLevel[level].getWidth(),
                  SpriteContents.this.byMipLevel[level].getHeight(),
                  texture,
                  0,
                  0,
                  SpriteContents.this.width >> level,
                  SpriteContents.this.height >> level,
                  level,
                  0
               );
            }

            frameTexturesByIndex.put(frame, RenderSystem.getDevice().createTextureView(texture));
         }

         for (int level = 0; level < SpriteContents.this.byMipLevel.length; level++) {
            spriteUbosByMip[level] = uboSlice.slice(level * spriteUboSize, spriteUboSize);
         }

         return SpriteContents.this.new AnimationState(this, frameTexturesByIndex, spriteUbosByMip);
      }

      public IntList getUniqueFrames() {
         return this.uniqueFrames;
      }
   }

   public class AnimationState implements AutoCloseable {
      private int frame;
      private int subFrame;
      private final SpriteContents.AnimatedTexture animationInfo;
      private final Int2ObjectMap<GpuTextureView> frameTexturesByIndex;
      private final GpuBufferSlice[] spriteUbosByMip;
      private boolean isDirty = true;

      private AnimationState(
         final SpriteContents.AnimatedTexture animationInfo, final Int2ObjectMap<GpuTextureView> frameTexturesByIndex, final GpuBufferSlice[] spriteUbosByMip
      ) {
         this.animationInfo = animationInfo;
         this.frameTexturesByIndex = frameTexturesByIndex;
         this.spriteUbosByMip = spriteUbosByMip;
      }

      public void tick() {
         this.subFrame++;
         this.isDirty = false;
         SpriteContents.FrameInfo currentFrame = this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= currentFrame.time) {
            int oldFrame = currentFrame.index;
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
            int newFrame = this.animationInfo.frames.get(this.frame).index;
            if (oldFrame != newFrame) {
               this.isDirty = true;
            }
         }
      }

      public GpuBufferSlice getDrawUbo(final int level) {
         return this.spriteUbosByMip[level];
      }

      public boolean needsToDraw() {
         return this.animationInfo.interpolateFrames || this.isDirty;
      }

      public void drawToAtlas(final RenderPass renderPass, final GpuBufferSlice ubo) {
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
         List<SpriteContents.FrameInfo> frames = this.animationInfo.frames;
         int oldFrame = frames.get(this.frame).index;
         float frameProgress = (float)this.subFrame / this.animationInfo.frames.get(this.frame).time;
         int frameProgressAsInt = (int)(frameProgress * 1000.0F);
         if (this.animationInfo.interpolateFrames) {
            int newFrame = frames.get((this.frame + 1) % frames.size()).index;
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE));
            renderPass.bindTexture("CurrentSprite", (GpuTextureView)this.frameTexturesByIndex.get(oldFrame), sampler);
            renderPass.bindTexture("NextSprite", (GpuTextureView)this.frameTexturesByIndex.get(newFrame), sampler);
         } else if (this.isDirty) {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT));
            renderPass.bindTexture("Sprite", (GpuTextureView)this.frameTexturesByIndex.get(oldFrame), sampler);
         }

         renderPass.setUniform("SpriteAnimationInfo", ubo);
         renderPass.draw(6, 1, frameProgressAsInt << 3, 0);
      }

      @Override
      public void close() {
         ObjectIterator var1 = this.frameTexturesByIndex.values().iterator();

         while (var1.hasNext()) {
            GpuTextureView view = (GpuTextureView)var1.next();
            view.texture().close();
            view.close();
         }
      }
   }

   private record FrameInfo(int index, int time) {
   }
}
