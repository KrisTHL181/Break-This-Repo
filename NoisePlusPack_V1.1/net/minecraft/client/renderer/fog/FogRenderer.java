package net.minecraft.client.renderer.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class FogRenderer implements AutoCloseable {
   public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
   private static final List<FogEnvironment> FOG_ENVIRONMENTS = Lists.newArrayList(
      new FogEnvironment[]{
         new LavaFogEnvironment(),
         new PowderedSnowFogEnvironment(),
         new BlindnessFogEnvironment(),
         new DarknessFogEnvironment(),
         new WaterFogEnvironment(),
         new AtmosphericFogEnvironment()
      }
   );
   private static boolean fogEnabled = true;
   private final GpuBuffer emptyBuffer;
   private final MappableRingBuffer regularBuffer;

   public FogRenderer() {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.regularBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         ByteBuffer bytebuffer = memorystack.malloc(FOG_UBO_SIZE);
         this.updateBuffer(
            bytebuffer, 0, new Vector4f(0.0F), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
         );
         this.emptyBuffer = gpudevice.createBuffer(() -> "Empty fog", 128, bytebuffer.flip());
      } catch (Throwable var6) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      RenderSystem.setShaderFog(this.getBuffer(FogRenderer.FogMode.NONE));
   }

   @Override
   public void close() {
      this.emptyBuffer.close();
      this.regularBuffer.close();
   }

   public void endFrame() {
      this.regularBuffer.rotate();
   }

   public GpuBufferSlice getBuffer(FogRenderer.FogMode p_408481_) {
      if (!fogEnabled) {
         return this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
      }

      return switch (p_408481_) {
         case NONE -> this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
         case WORLD -> this.regularBuffer.currentBuffer().slice(0L, FOG_UBO_SIZE);
      };
   }

   private Vector4f computeFogColor(Camera p_407993_, float p_409359_, ClientLevel p_409862_, int p_409400_, float p_406105_) {
      FogType fogtype = this.getFogType(p_407993_);
      Entity entity = p_407993_.entity();
      FogEnvironment fogenvironment = null;
      FogEnvironment fogenvironment1 = null;

      for (FogEnvironment fogenvironment2 : FOG_ENVIRONMENTS) {
         if (fogenvironment2.isApplicable(fogtype, entity)) {
            if (fogenvironment == null && fogenvironment2.providesColor()) {
               fogenvironment = fogenvironment2;
            }

            if (fogenvironment1 == null && fogenvironment2.modifiesDarkness()) {
               fogenvironment1 = fogenvironment2;
            }
         }
      }

      if (fogenvironment == null) {
         throw new IllegalStateException("No color source environment found");
      }

      int i = fogenvironment.getBaseColor(p_409862_, p_407993_, p_409400_, p_409359_);
      float f4 = p_409862_.getLevelData().voidDarknessOnsetRange();
      float f = Mth.clamp((f4 + p_409862_.getMinY() - (float)p_407993_.position().y) / f4, 0.0F, 1.0F);
      if (fogenvironment1 != null) {
         LivingEntity livingentity = (LivingEntity)entity;
         f = fogenvironment1.getModifiedDarkness(livingentity, f, p_409359_);
      }

      float f5 = ARGB.redFloat(i);
      float f1 = ARGB.greenFloat(i);
      float f2 = ARGB.blueFloat(i);
      if (f > 0.0F && fogtype != FogType.LAVA && fogtype != FogType.POWDER_SNOW) {
         float f3 = Mth.square(1.0F - f);
         f5 *= f3;
         f1 *= f3;
         f2 *= f3;
      }

      if (p_406105_ > 0.0F) {
         f5 = Mth.lerp(p_406105_, f5, f5 * 0.7F);
         f1 = Mth.lerp(p_406105_, f1, f1 * 0.6F);
         f2 = Mth.lerp(p_406105_, f2, f2 * 0.6F);
      }

      float f6;
      if (fogtype == FogType.WATER) {
         if (entity instanceof LocalPlayer) {
            f6 = ((LocalPlayer)entity).getWaterVision();
         } else {
            f6 = 1.0F;
         }
      } else if (entity instanceof LivingEntity livingentity1
         && livingentity1.hasEffect(MobEffects.NIGHT_VISION)
         && !livingentity1.hasEffect(MobEffects.DARKNESS)) {
         f6 = GameRenderer.getNightVisionScale(livingentity1, p_409359_);
      } else {
         f6 = 0.0F;
      }

      if (f5 != 0.0F && f1 != 0.0F && f2 != 0.0F) {
         float f7 = 1.0F / Math.max(f5, Math.max(f1, f2));
         f5 = Mth.lerp(f6, f5, f5 * f7);
         f1 = Mth.lerp(f6, f1, f1 * f7);
         f2 = Mth.lerp(f6, f2, f2 * f7);
      }

      return new Vector4f(f5, f1, f2, 1.0F);
   }

   public static boolean toggleFog() {
      return fogEnabled = !fogEnabled;
   }

   public Vector4f setupFog(Camera p_407701_, int p_407051_, DeltaTracker p_406871_, float p_409892_, ClientLevel p_407936_) {
      float f = p_406871_.getGameTimeDeltaPartialTick(false);
      Vector4f vector4f = this.computeFogColor(p_407701_, f, p_407936_, p_407051_, p_409892_);
      float f1 = p_407051_ * 16;
      FogType fogtype = this.getFogType(p_407701_);
      Entity entity = p_407701_.entity();
      FogData fogdata = new FogData();

      for (FogEnvironment fogenvironment : FOG_ENVIRONMENTS) {
         if (fogenvironment.isApplicable(fogtype, entity)) {
            fogenvironment.setupFog(fogdata, p_407701_, p_407936_, f1, p_406871_);
            break;
         }
      }

      float f2 = Mth.clamp(f1 / 10.0F, 4.0F, 64.0F);
      fogdata.renderDistanceStart = f1 - f2;
      fogdata.renderDistanceEnd = f1;

      try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
            .createCommandEncoder()
            .mapBuffer(this.regularBuffer.currentBuffer(), false, true)) {
         this.updateBuffer(
            gpubuffer$mappedview.data(),
            0,
            vector4f,
            fogdata.environmentalStart,
            fogdata.environmentalEnd,
            fogdata.renderDistanceStart,
            fogdata.renderDistanceEnd,
            fogdata.skyEnd,
            fogdata.cloudEnd
         );
      }

      return vector4f;
   }

   private FogType getFogType(Camera p_409791_) {
      FogType fogtype = p_409791_.getFluidInCamera();
      return fogtype == FogType.NONE ? FogType.ATMOSPHERIC : fogtype;
   }

   private void updateBuffer(
      ByteBuffer p_410444_,
      int p_407998_,
      Vector4f p_407958_,
      float p_408373_,
      float p_408335_,
      float p_408455_,
      float p_408170_,
      float p_408550_,
      float p_406808_
   ) {
      p_410444_.position(p_407998_);
      Std140Builder.intoBuffer(p_410444_)
         .putVec4(p_407958_)
         .putFloat(p_408373_)
         .putFloat(p_408335_)
         .putFloat(p_408455_)
         .putFloat(p_408170_)
         .putFloat(p_408550_)
         .putFloat(p_406808_);
   }

   @OnlyIn(Dist.CLIENT)
   public enum FogMode {
      NONE,
      WORLD;
   }
}
