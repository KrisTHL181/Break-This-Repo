package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class LightTexture implements AutoCloseable {
   public static final int FULL_BRIGHT = 15728880;
   public static final int FULL_SKY = 15728640;
   public static final int FULL_BLOCK = 240;
   private static final int TEXTURE_SIZE = 16;
   private static final int LIGHTMAP_UBO_SIZE = new Std140SizeCalculator()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putVec3()
      .putVec3()
      .get();
   private final GpuTexture texture;
   private final GpuTextureView textureView;
   private boolean updateLightTexture;
   private float blockLightRedFlicker;
   private final GameRenderer renderer;
   private final Minecraft minecraft;
   private final MappableRingBuffer ubo;
   private final RandomSource randomSource = RandomSource.create();

   public LightTexture(GameRenderer p_109878_, Minecraft p_109879_) {
      this.renderer = p_109878_;
      this.minecraft = p_109879_;
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture("Light Texture", 12, TextureFormat.RGBA8, 16, 16, 1, 1);
      this.textureView = gpudevice.createTextureView(this.texture);
      gpudevice.createCommandEncoder().clearColorTexture(this.texture, -1);
      this.ubo = new MappableRingBuffer(() -> "Lightmap UBO", 130, LIGHTMAP_UBO_SIZE);
   }

   public GpuTextureView getTextureView() {
      return this.textureView;
   }

   @Override
   public void close() {
      this.texture.close();
      this.textureView.close();
      this.ubo.close();
   }

   public void tick() {
      this.blockLightRedFlicker = this.blockLightRedFlicker
         + (this.randomSource.nextFloat() - this.randomSource.nextFloat()) * this.randomSource.nextFloat() * this.randomSource.nextFloat() * 0.1F;
      this.blockLightRedFlicker *= 0.9F;
      this.updateLightTexture = true;
   }

   private float calculateDarknessScale(LivingEntity p_234313_, float p_234314_, float p_234315_) {
      float f = 0.45F * p_234314_;
      return Math.max(0.0F, Mth.cos((p_234313_.tickCount - p_234315_) * (float) Math.PI * 0.025F) * f);
   }

   public void updateLightTexture(float p_109882_) {
      if (this.updateLightTexture) {
         this.updateLightTexture = false;
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("lightTex");
         ClientLevel clientlevel = this.minecraft.level;
         if (clientlevel != null) {
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            int i = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, p_109882_);
            float f = clientlevel.dimensionType().ambientLight();
            float f1 = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, p_109882_);
            EndFlashState endflashstate = clientlevel.endFlashState();
            Vector3f vector3f;
            if (endflashstate != null) {
               vector3f = new Vector3f(0.99F, 1.12F, 1.0F);
               if (!this.minecraft.options.hideLightningFlash().get()) {
                  float f2 = endflashstate.getIntensity(p_109882_);
                  if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                     f1 += f2 / 3.0F;
                  } else {
                     f1 += f2;
                  }
               }
            } else {
               vector3f = new Vector3f(1.0F, 1.0F, 1.0F);
            }

            float f9 = this.minecraft.options.darknessEffectScale().get().floatValue();
            float f3 = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, p_109882_) * f9;
            float f4 = this.calculateDarknessScale(this.minecraft.player, f3, p_109882_) * f9;
            float f6 = this.minecraft.player.getWaterVision();
            float f5;
            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
               f5 = GameRenderer.getNightVisionScale(this.minecraft.player, p_109882_);
            } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
               f5 = f6;
            } else {
               f5 = 0.0F;
            }

            float f7 = this.blockLightRedFlicker + 1.5F;
            float f8 = this.minecraft.options.gamma().get().floatValue();
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

            try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
               Std140Builder.intoBuffer(gpubuffer$mappedview.data())
                  .putFloat(f)
                  .putFloat(f1)
                  .putFloat(f7)
                  .putFloat(f5)
                  .putFloat(f4)
                  .putFloat(this.renderer.getDarkenWorldAmount(p_109882_))
                  .putFloat(Math.max(0.0F, f8 - f3))
                  .putVec3(ARGB.vector3fFromRGB24(i))
                  .putVec3(vector3f);
            }

            try (RenderPass renderpass = commandencoder.createRenderPass(() -> "Update light", this.textureView, OptionalInt.empty())) {
               renderpass.setPipeline(RenderPipelines.LIGHTMAP);
               RenderSystem.bindDefaultUniforms(renderpass);
               renderpass.setUniform("LightmapInfo", this.ubo.currentBuffer());
               renderpass.draw(0, 3);
            }

            this.ubo.rotate();
            profilerfiller.pop();
         }
      }
   }

   public static float getBrightness(DimensionType p_234317_, int p_234318_) {
      return getBrightness(p_234317_.ambientLight(), p_234318_);
   }

   public static float getBrightness(float p_362774_, int p_368270_) {
      float f = p_368270_ / 15.0F;
      float f1 = f / (4.0F - 3.0F * f);
      return Mth.lerp(p_362774_, f1, 1.0F);
   }

   public static int pack(int p_109886_, int p_109887_) {
      return p_109886_ << 4 | p_109887_ << 20;
   }

   public static int block(int p_109884_) {
      return p_109884_ >>> 4 & 15;
   }

   public static int sky(int p_109895_) {
      return p_109895_ >>> 20 & 15;
   }

   public static int lightCoordsWithEmission(int p_363075_, int p_361575_) {
      if (p_361575_ == 0) {
         return p_363075_;
      }

      int i = Math.max(sky(p_363075_), p_361575_);
      int j = Math.max(block(p_363075_), p_361575_);
      return pack(j, i);
   }
}
