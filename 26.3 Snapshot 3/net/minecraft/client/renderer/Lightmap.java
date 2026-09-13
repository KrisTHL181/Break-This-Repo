package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector4fc;

public class Lightmap implements AutoCloseable {
   public static final int TEXTURE_SIZE = 16;
   private static final Vector4fc CLEAR_COLOR = ARGB.vector4fFromARGB32(-1);
   private static final int LIGHTMAP_UBO_SIZE = new Std140SizeCalculator()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putVec3()
      .putVec3()
      .putVec3()
      .putVec3()
      .get();
   private final GpuTexture texture;
   private final GpuTextureView textureView;
   private final MappableRingBuffer ubo;

   public Lightmap() {
      GpuDevice device = RenderSystem.getDevice();
      this.texture = device.createTexture("Lightmap", 13, GpuFormat.RGBA8_UNORM, 16, 16, 1, 1);
      this.textureView = device.createTextureView(this.texture);
      device.createCommandEncoder().clearColorTexture(this.texture, CLEAR_COLOR);
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

   public void render(final LightmapRenderState renderState) {
      if (renderState.needsUpdate) {
         ProfilerFiller profiler = Profiler.get();
         profiler.push("lightmap");
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

         try (GpuBufferSlice.MappedView view = this.ubo.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(view.data())
               .putFloat(renderState.skyFactor)
               .putFloat(renderState.blockFactor)
               .putFloat(renderState.nightVisionEffectIntensity)
               .putFloat(renderState.darknessEffectScale)
               .putFloat(renderState.bossOverlayWorldDarkening)
               .putFloat(renderState.brightness)
               .putVec3(renderState.blockLightTint)
               .putVec3(renderState.skyLightColor)
               .putVec3(renderState.ambientColor)
               .putVec3(renderState.nightVisionColor);
         }

         try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "Update light", this.textureView, Optional.empty())) {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.LIGHTMAP));
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("LightmapInfo", this.ubo.currentBuffer());
            renderPass.draw(3, 1, 0, 0);
         }

         this.ubo.rotate();
         profiler.pop();
      }
   }

   public static float getBrightness(final DimensionType dimensionType, final int level) {
      float v = level / 15.0F;
      float curvedV = v / (4.0F - 3.0F * v);
      return Mth.lerp(dimensionType.ambientLight(), curvedV, 1.0F);
   }
}
