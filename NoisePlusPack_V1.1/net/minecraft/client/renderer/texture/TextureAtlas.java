package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureAtlas extends AbstractTexture implements Dumpable, TickableTexture {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Deprecated
   public static final Identifier LOCATION_BLOCKS = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
   @Deprecated
   public static final Identifier LOCATION_ITEMS = Identifier.withDefaultNamespace("textures/atlas/items.png");
   @Deprecated
   public static final Identifier LOCATION_PARTICLES = Identifier.withDefaultNamespace("textures/atlas/particles.png");
   private List<TextureAtlasSprite> sprites = List.of();
   private List<SpriteContents.AnimationState> animatedTexturesStates = List.of();
   private Map<Identifier, TextureAtlasSprite> texturesByName = Map.of();
   private @Nullable TextureAtlasSprite missingSprite;
   private final Identifier location;
   private final int maxSupportedTextureSize;
   private int width;
   private int height;
   private int maxMipLevel;
   private int mipLevelCount;
   private GpuTextureView[] mipViews = new GpuTextureView[0];
   private @Nullable GpuBuffer spriteUbos;

   public TextureAtlas(Identifier p_458227_) {
      this.location = p_458227_;
      this.maxSupportedTextureSize = RenderSystem.getDevice().getMaxTextureSize();
   }

   private void createTexture(int p_410800_, int p_410805_, int p_410791_) {
      LOGGER.info("Created: {}x{}x{} {}-atlas", new Object[]{p_410800_, p_410805_, p_410791_, this.location});
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.close();
      this.texture = gpudevice.createTexture(this.location::toString, 15, TextureFormat.RGBA8, p_410800_, p_410805_, 1, p_410791_ + 1);
      this.textureView = gpudevice.createTextureView(this.texture);
      this.width = p_410800_;
      this.height = p_410805_;
      this.maxMipLevel = p_410791_;
      this.mipLevelCount = p_410791_ + 1;
      this.mipViews = new GpuTextureView[this.mipLevelCount];

      for (int i = 0; i <= this.maxMipLevel; i++) {
         this.mipViews[i] = gpudevice.createTextureView(this.texture, i, 1);
      }
   }

   public void upload(SpriteLoader.Preparations p_250662_) {
      this.createTexture(p_250662_.width(), p_250662_.height(), p_250662_.mipLevel());
      this.clearTextureData();
      this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
      this.texturesByName = Map.copyOf(p_250662_.regions());
      this.missingSprite = this.texturesByName.get(MissingTextureAtlasSprite.getLocation());
      if (this.missingSprite == null) {
         throw new IllegalStateException("Atlas '" + this.location + "' (" + this.texturesByName.size() + " sprites) has no missing texture sprite");
      }

      List<TextureAtlasSprite> list = new ArrayList<>();
      List<SpriteContents.AnimationState> list1 = new ArrayList<>();
      int i = (int)p_250662_.regions().values().stream().filter(TextureAtlasSprite::isAnimated).count();
      int j = Mth.roundToward(SpriteContents.UBO_SIZE, RenderSystem.getDevice().getUniformOffsetAlignment());
      int k = j * this.mipLevelCount;
      ByteBuffer bytebuffer = MemoryUtil.memAlloc(i * k);
      int l = 0;

      for (TextureAtlasSprite textureatlassprite : p_250662_.regions().values()) {
         if (textureatlassprite.isAnimated()) {
            textureatlassprite.uploadSpriteUbo(bytebuffer, l * k, this.maxMipLevel, this.width, this.height, j);
            l++;
         }
      }

      GpuBuffer gpubuffer = l > 0 ? RenderSystem.getDevice().createBuffer(() -> this.location + " sprite UBOs", 128, bytebuffer) : null;
      l = 0;

      for (TextureAtlasSprite textureatlassprite1 : p_250662_.regions().values()) {
         list.add(textureatlassprite1);
         if (textureatlassprite1.isAnimated() && gpubuffer != null) {
            SpriteContents.AnimationState spritecontents$animationstate = textureatlassprite1.createAnimationState(gpubuffer.slice(l * k, k), j);
            l++;
            if (spritecontents$animationstate != null) {
               list1.add(spritecontents$animationstate);
            }
         }
      }

      this.spriteUbos = gpubuffer;
      this.sprites = list;
      this.animatedTexturesStates = List.copyOf(list1);
      this.uploadInitialContents();
      if (SharedConstants.DEBUG_DUMP_TEXTURE_ATLAS) {
         Path path = TextureUtil.getDebugTexturePath();

         try {
            Files.createDirectories(path);
            this.dumpContents(this.location, path);
         } catch (Exception exception) {
            LOGGER.warn("Failed to dump atlas contents to {}", path);
         }
      }
   }

   private void uploadInitialContents() {
      GpuDevice gpudevice = RenderSystem.getDevice();
      int i = Mth.roundToward(SpriteContents.UBO_SIZE, RenderSystem.getDevice().getUniformOffsetAlignment());
      int j = i * this.mipLevelCount;
      GpuSampler gpusampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
      List<TextureAtlasSprite> list = this.sprites.stream().filter(p_448390_ -> !p_448390_.isAnimated()).toList();
      List<GpuTextureView[]> list1 = new ArrayList<>();
      ByteBuffer bytebuffer = MemoryUtil.memAlloc(list.size() * j);

      for (int k = 0; k < list.size(); k++) {
         TextureAtlasSprite textureatlassprite = list.get(k);
         textureatlassprite.uploadSpriteUbo(bytebuffer, k * j, this.maxMipLevel, this.width, this.height, i);
         GpuTexture gputexture = gpudevice.createTexture(
            () -> textureatlassprite.contents().name().toString(),
            5,
            TextureFormat.RGBA8,
            textureatlassprite.contents().width(),
            textureatlassprite.contents().height(),
            1,
            this.mipLevelCount
         );
         GpuTextureView[] agputextureview = new GpuTextureView[this.mipLevelCount];

         for (int l = 0; l <= this.maxMipLevel; l++) {
            textureatlassprite.uploadFirstFrame(gputexture, l);
            agputextureview[l] = gpudevice.createTextureView(gputexture);
         }

         list1.add(agputextureview);
      }

      try (GpuBuffer gpubuffer = gpudevice.createBuffer(() -> "SpriteAnimationInfo", 128, bytebuffer)) {
         for (int i1 = 0; i1 < this.mipLevelCount; i1++) {
            try (RenderPass renderpass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "Animate " + this.location, this.mipViews[i1], OptionalInt.empty())) {
               renderpass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);

               for (int j1 = 0; j1 < list.size(); j1++) {
                  renderpass.bindTexture("Sprite", list1.get(j1)[i1], gpusampler);
                  renderpass.setUniform("SpriteAnimationInfo", gpubuffer.slice(j1 * j + i1 * i, SpriteContents.UBO_SIZE));
                  renderpass.draw(0, 6);
               }
            }
         }
      }

      for (GpuTextureView[] agputextureview1 : list1) {
         for (GpuTextureView gputextureview : agputextureview1) {
            gputextureview.close();
            gputextureview.texture().close();
         }
      }

      MemoryUtil.memFree(bytebuffer);
      this.uploadAnimationFrames();
   }

   @Override
   public void dumpContents(Identifier p_450858_, Path p_276127_) throws IOException {
      String s = p_450858_.toDebugFileName();
      TextureUtil.writeAsPNG(p_276127_, s, this.getTexture(), this.maxMipLevel, p_395843_ -> p_395843_);
      dumpSpriteNames(p_276127_, s, this.texturesByName);
   }

   private static void dumpSpriteNames(Path p_261769_, String p_262102_, Map<Identifier, TextureAtlasSprite> p_261722_) {
      Path path = p_261769_.resolve(p_262102_ + ".txt");

      try (Writer writer = Files.newBufferedWriter(path)) {
         for (Entry<Identifier, TextureAtlasSprite> entry : p_261722_.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
            TextureAtlasSprite textureatlassprite = entry.getValue();
            writer.write(
               String.format(
                  Locale.ROOT,
                  "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                  entry.getKey(),
                  textureatlassprite.getX(),
                  textureatlassprite.getY(),
                  textureatlassprite.contents().width(),
                  textureatlassprite.contents().height()
               )
            );
         }
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to write file {}", path, ioexception);
      }
   }

   public void cycleAnimationFrames() {
      if (this.texture != null) {
         for (SpriteContents.AnimationState spritecontents$animationstate : this.animatedTexturesStates) {
            spritecontents$animationstate.tick();
         }

         this.uploadAnimationFrames();
      }
   }

   private void uploadAnimationFrames() {
      if (this.animatedTexturesStates.stream().anyMatch(SpriteContents.AnimationState::needsToDraw)) {
         for (int i = 0; i <= this.maxMipLevel; i++) {
            try (RenderPass renderpass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "Animate " + this.location, this.mipViews[i], OptionalInt.empty())) {
               for (SpriteContents.AnimationState spritecontents$animationstate : this.animatedTexturesStates) {
                  if (spritecontents$animationstate.needsToDraw()) {
                     spritecontents$animationstate.drawToAtlas(renderpass, spritecontents$animationstate.getDrawUbo(i));
                  }
               }
            }
         }
      }
   }

   @Override
   public void tick() {
      this.cycleAnimationFrames();
   }

   public TextureAtlasSprite getSprite(Identifier p_455105_) {
      TextureAtlasSprite textureatlassprite = this.texturesByName.getOrDefault(p_455105_, this.missingSprite);
      if (textureatlassprite == null) {
         throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
      } else {
         return textureatlassprite;
      }
   }

   public TextureAtlasSprite missingSprite() {
      return Objects.requireNonNull(this.missingSprite, "Atlas not initialized");
   }

   public void clearTextureData() {
      this.sprites.forEach(TextureAtlasSprite::close);
      this.sprites = List.of();
      this.animatedTexturesStates = List.of();
      this.texturesByName = Map.of();
      this.missingSprite = null;
   }

   @Override
   public void close() {
      super.close();

      for (GpuTextureView gputextureview : this.mipViews) {
         gputextureview.close();
      }

      for (SpriteContents.AnimationState spritecontents$animationstate : this.animatedTexturesStates) {
         spritecontents$animationstate.close();
      }

      if (this.spriteUbos != null) {
         this.spriteUbos.close();
         this.spriteUbos = null;
      }
   }

   public Identifier location() {
      return this.location;
   }

   public int maxSupportedTextureSize() {
      return this.maxSupportedTextureSize;
   }

   int getWidth() {
      return this.width;
   }

   int getHeight() {
      return this.height;
   }
}
