package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteContents implements Stitcher.Entry, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int UBO_SIZE = new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();
   final Identifier name;
   final int width;
   final int height;
   private final NativeImage originalImage;
   NativeImage[] byMipLevel;
   private final SpriteContents.@Nullable AnimatedTexture animatedTexture;
   private final List<MetadataSectionType.WithValue<?>> additionalMetadata;
   private final MipmapStrategy mipmapStrategy;
   private final float alphaCutoffBias;

   public SpriteContents(Identifier p_452618_, FrameSize p_423886_, NativeImage p_422833_) {
      this(p_452618_, p_423886_, p_422833_, Optional.empty(), List.of(), Optional.empty());
   }

   public SpriteContents(
      Identifier p_452840_,
      FrameSize p_251031_,
      NativeImage p_252131_,
      Optional<AnimationMetadataSection> p_428820_,
      List<MetadataSectionType.WithValue<?>> p_424177_,
      Optional<TextureMetadataSection> p_460315_
   ) {
      this.name = p_452840_;
      this.width = p_251031_.width();
      this.height = p_251031_.height();
      this.additionalMetadata = p_424177_;
      this.animatedTexture = p_428820_.<SpriteContents.AnimatedTexture>map(
            p_374666_ -> this.createAnimatedTexture(p_251031_, p_252131_.getWidth(), p_252131_.getHeight(), p_374666_)
         )
         .orElse(null);
      this.originalImage = p_252131_;
      this.byMipLevel = new NativeImage[]{this.originalImage};
      this.mipmapStrategy = p_460315_.map(TextureMetadataSection::mipmapStrategy).orElse(MipmapStrategy.AUTO);
      this.alphaCutoffBias = p_460315_.map(TextureMetadataSection::alphaCutoffBias).orElse(0.0F);
   }

   public void increaseMipLevel(int p_248864_) {
      try {
         this.byMipLevel = MipmapGenerator.generateMipLevels(this.name, this.byMipLevel, p_248864_, this.mipmapStrategy, this.alphaCutoffBias);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Generating mipmaps for frame");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Frame being iterated");
         crashreportcategory.setDetail("Sprite name", this.name);
         crashreportcategory.setDetail("Sprite size", () -> this.width + " x " + this.height);
         crashreportcategory.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
         crashreportcategory.setDetail("Mipmap levels", p_248864_);
         crashreportcategory.setDetail("Original image size", () -> this.originalImage.getWidth() + "x" + this.originalImage.getHeight());
         throw new ReportedException(crashreport);
      }
   }

   private int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   public boolean isAnimated() {
      return this.getFrameCount() > 1;
   }

   private SpriteContents.@Nullable AnimatedTexture createAnimatedTexture(FrameSize p_250817_, int p_249792_, int p_252353_, AnimationMetadataSection p_250947_) {
      int i = p_249792_ / p_250817_.width();
      int j = p_252353_ / p_250817_.height();
      int k = i * j;
      int l = p_250947_.defaultFrameTime();
      List<SpriteContents.FrameInfo> list;
      if (p_250947_.frames().isEmpty()) {
         list = new ArrayList<>(k);

         for (int i1 = 0; i1 < k; i1++) {
            list.add(new SpriteContents.FrameInfo(i1, l));
         }
      } else {
         List<AnimationFrame> list1 = p_250947_.frames().get();
         list = new ArrayList<>(list1.size());

         for (AnimationFrame animationframe : list1) {
            list.add(new SpriteContents.FrameInfo(animationframe.index(), animationframe.timeOr(l)));
         }

         int j1 = 0;
         IntSet intset = new IntOpenHashSet();

         for (Iterator<SpriteContents.FrameInfo> iterator = list.iterator(); iterator.hasNext(); j1++) {
            SpriteContents.FrameInfo spritecontents$frameinfo = iterator.next();
            boolean flag = true;
            if (spritecontents$frameinfo.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.name, j1, spritecontents$frameinfo.time});
               flag = false;
            }

            if (spritecontents$frameinfo.index < 0 || spritecontents$frameinfo.index >= k) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.name, j1, spritecontents$frameinfo.index});
               flag = false;
            }

            if (flag) {
               intset.add(spritecontents$frameinfo.index);
            } else {
               iterator.remove();
            }
         }

         int[] aint = IntStream.range(0, k).filter(p_251185_ -> !intset.contains(p_251185_)).toArray();
         if (aint.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(aint));
         }
      }

      return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(list), i, p_250947_.interpolatedFrames());
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

   public IntStream getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
   }

   public SpriteContents.@Nullable AnimationState createAnimationState(GpuBufferSlice p_454298_, int p_459047_) {
      return this.animatedTexture != null ? this.animatedTexture.createAnimationState(p_454298_, p_459047_) : null;
   }

   public <T> Optional<T> getAdditionalMetadata(MetadataSectionType<T> p_424898_) {
      for (MetadataSectionType.WithValue<?> withvalue : this.additionalMetadata) {
         Optional<T> optional = withvalue.unwrapToType(p_424898_);
         if (optional.isPresent()) {
            return optional;
         }
      }

      return Optional.empty();
   }

   @Override
   public void close() {
      for (NativeImage nativeimage : this.byMipLevel) {
         nativeimage.close();
      }
   }

   @Override
   public String toString() {
      return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isTransparent(int p_250374_, int p_250934_, int p_249573_) {
      int i = p_250934_;
      int j = p_249573_;
      if (this.animatedTexture != null) {
         i = p_250934_ + this.animatedTexture.getFrameX(p_250374_) * this.width;
         j = p_249573_ + this.animatedTexture.getFrameY(p_250374_) * this.height;
      }

      return ARGB.alpha(this.originalImage.getPixel(i, j)) == 0;
   }

   public void uploadFirstFrame(GpuTexture p_394515_, int p_252315_) {
      RenderSystem.getDevice()
         .createCommandEncoder()
         .writeToTexture(p_394515_, this.byMipLevel[p_252315_], p_252315_, 0, 0, 0, this.width >> p_252315_, this.height >> p_252315_, 0, 0);
   }

   @OnlyIn(Dist.CLIENT)
   class AnimatedTexture {
      final List<SpriteContents.FrameInfo> frames;
      private final int frameRowSize;
      final boolean interpolateFrames;

      AnimatedTexture(final List<SpriteContents.FrameInfo> p_250968_, final int p_251686_, final boolean p_251832_) {
         this.frames = p_250968_;
         this.frameRowSize = p_251686_;
         this.interpolateFrames = p_251832_;
      }

      int getFrameX(int p_249475_) {
         return p_249475_ % this.frameRowSize;
      }

      int getFrameY(int p_251327_) {
         return p_251327_ / this.frameRowSize;
      }

      public SpriteContents.AnimationState createAnimationState(GpuBufferSlice p_450267_, int p_460446_) {
         GpuDevice gpudevice = RenderSystem.getDevice();
         Int2ObjectMap<GpuTextureView> int2objectmap = new Int2ObjectOpenHashMap();
         GpuBufferSlice[] agpubufferslice = new GpuBufferSlice[SpriteContents.this.byMipLevel.length];

         for (int i : this.getUniqueFrames().toArray()) {
            GpuTexture gputexture = gpudevice.createTexture(
               () -> SpriteContents.this.name + " animation frame " + i,
               5,
               TextureFormat.RGBA8,
               SpriteContents.this.width,
               SpriteContents.this.height,
               1,
               SpriteContents.this.byMipLevel.length + 1
            );
            int j = this.getFrameX(i) * SpriteContents.this.width;
            int k = this.getFrameY(i) * SpriteContents.this.height;

            for (int l = 0; l < SpriteContents.this.byMipLevel.length; l++) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToTexture(
                     gputexture, SpriteContents.this.byMipLevel[l], l, 0, 0, 0, SpriteContents.this.width >> l, SpriteContents.this.height >> l, j >> l, k >> l
                  );
            }

            int2objectmap.put(i, RenderSystem.getDevice().createTextureView(gputexture));
         }

         for (int i1 = 0; i1 < SpriteContents.this.byMipLevel.length; i1++) {
            agpubufferslice[i1] = p_450267_.slice(i1 * p_460446_, p_460446_);
         }

         return SpriteContents.this.new AnimationState(this, int2objectmap, agpubufferslice);
      }

      public IntStream getUniqueFrames() {
         return this.frames.stream().mapToInt(p_249981_ -> p_249981_.index).distinct();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class AnimationState implements AutoCloseable {
      private int frame;
      private int subFrame;
      private final SpriteContents.AnimatedTexture animationInfo;
      private final Int2ObjectMap<GpuTextureView> frameTexturesByIndex;
      private final GpuBufferSlice[] spriteUbosByMip;
      private boolean isDirty = true;

      AnimationState(final SpriteContents.AnimatedTexture p_452987_, final Int2ObjectMap<GpuTextureView> p_452549_, final GpuBufferSlice[] p_453889_) {
         this.animationInfo = p_452987_;
         this.frameTexturesByIndex = p_452549_;
         this.spriteUbosByMip = p_453889_;
      }

      public void tick() {
         this.subFrame++;
         this.isDirty = false;
         SpriteContents.FrameInfo spritecontents$frameinfo = this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= spritecontents$frameinfo.time) {
            int i = spritecontents$frameinfo.index;
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
            int j = this.animationInfo.frames.get(this.frame).index;
            if (i != j) {
               this.isDirty = true;
            }
         }
      }

      public GpuBufferSlice getDrawUbo(int p_454619_) {
         return this.spriteUbosByMip[p_454619_];
      }

      public boolean needsToDraw() {
         return this.animationInfo.interpolateFrames || this.isDirty;
      }

      public void drawToAtlas(RenderPass p_460349_, GpuBufferSlice p_456503_) {
         GpuSampler gpusampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
         List<SpriteContents.FrameInfo> list = this.animationInfo.frames;
         int i = list.get(this.frame).index;
         float f = (float)this.subFrame / this.animationInfo.frames.get(this.frame).time;
         int j = (int)(f * 1000.0F);
         if (this.animationInfo.interpolateFrames) {
            int k = list.get((this.frame + 1) % list.size()).index;
            p_460349_.setPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE);
            p_460349_.bindTexture("CurrentSprite", (GpuTextureView)this.frameTexturesByIndex.get(i), gpusampler);
            p_460349_.bindTexture("NextSprite", (GpuTextureView)this.frameTexturesByIndex.get(k), gpusampler);
         } else if (this.isDirty) {
            p_460349_.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);
            p_460349_.bindTexture("Sprite", (GpuTextureView)this.frameTexturesByIndex.get(i), gpusampler);
         }

         p_460349_.setUniform("SpriteAnimationInfo", p_456503_);
         p_460349_.draw(j << 3, 6);
      }

      @Override
      public void close() {
         ObjectIterator var1 = this.frameTexturesByIndex.values().iterator();

         while (var1.hasNext()) {
            GpuTextureView gputextureview = (GpuTextureView)var1.next();
            gputextureview.texture().close();
            gputextureview.close();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record FrameInfo(int index, int time) {
   }
}
