package net.minecraft.client.renderer.rendertype;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RenderSetup {
   final RenderPipeline pipeline;
   final Map<String, RenderSetup.TextureBinding> textures;
   final TextureTransform textureTransform;
   final OutputTarget outputTarget;
   final RenderSetup.OutlineProperty outlineProperty;
   final boolean useLightmap;
   final boolean useOverlay;
   final boolean affectsCrumbling;
   final boolean sortOnUpload;
   final int bufferSize;
   final LayeringTransform layeringTransform;

   RenderSetup(
      RenderPipeline p_455545_,
      Map<String, RenderSetup.TextureBinding> p_455652_,
      boolean p_459117_,
      boolean p_460590_,
      LayeringTransform p_458911_,
      OutputTarget p_457054_,
      TextureTransform p_453948_,
      RenderSetup.OutlineProperty p_454071_,
      boolean p_455910_,
      boolean p_450946_,
      int p_453755_
   ) {
      this.pipeline = p_455545_;
      this.textures = p_455652_;
      this.outputTarget = p_457054_;
      this.textureTransform = p_453948_;
      this.useLightmap = p_459117_;
      this.useOverlay = p_460590_;
      this.outlineProperty = p_454071_;
      this.layeringTransform = p_458911_;
      this.affectsCrumbling = p_455910_;
      this.sortOnUpload = p_450946_;
      this.bufferSize = p_453755_;
   }

   @Override
   public String toString() {
      return "RenderSetup[layeringTransform="
         + this.layeringTransform
         + ", textureTransform="
         + this.textureTransform
         + ", textures="
         + this.textures
         + ", outlineProperty="
         + this.outlineProperty
         + ", useLightmap="
         + this.useLightmap
         + ", useOverlay="
         + this.useOverlay
         + "]";
   }

   public static RenderSetup.RenderSetupBuilder builder(RenderPipeline p_455597_) {
      return new RenderSetup.RenderSetupBuilder(p_455597_);
   }

   public Map<String, RenderSetup.TextureAndSampler> getTextures() {
      if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap) {
         return Collections.emptyMap();
      }

      Map<String, RenderSetup.TextureAndSampler> map = new HashMap<>();
      if (this.useOverlay) {
         map.put(
            "Sampler1",
            new RenderSetup.TextureAndSampler(
               Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            )
         );
      }

      if (this.useLightmap) {
         map.put(
            "Sampler2",
            new RenderSetup.TextureAndSampler(
               Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            )
         );
      }

      TextureManager texturemanager = Minecraft.getInstance().getTextureManager();

      for (Entry<String, RenderSetup.TextureBinding> entry : this.textures.entrySet()) {
         AbstractTexture abstracttexture = texturemanager.getTexture(entry.getValue().location);
         GpuSampler gpusampler = entry.getValue().sampler().get();
         map.put(
            entry.getKey(), new RenderSetup.TextureAndSampler(abstracttexture.getTextureView(), gpusampler != null ? gpusampler : abstracttexture.getSampler())
         );
      }

      return map;
   }

   @OnlyIn(Dist.CLIENT)
   public enum OutlineProperty {
      NONE("none"),
      IS_OUTLINE("is_outline"),
      AFFECTS_OUTLINE("affects_outline");

      private final String name;

      OutlineProperty(final String p_452545_) {
         this.name = p_452545_;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class RenderSetupBuilder {
      private final RenderPipeline pipeline;
      private boolean useLightmap = false;
      private boolean useOverlay = false;
      private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
      private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
      private TextureTransform textureTransform = TextureTransform.DEFAULT_TEXTURING;
      private boolean affectsCrumbling = false;
      private boolean sortOnUpload = false;
      private int bufferSize = 1536;
      private RenderSetup.OutlineProperty outlineProperty = RenderSetup.OutlineProperty.NONE;
      private final Map<String, RenderSetup.TextureBinding> textures = new HashMap<>();

      RenderSetupBuilder(RenderPipeline p_453804_) {
         this.pipeline = p_453804_;
      }

      public RenderSetup.RenderSetupBuilder withTexture(String p_450834_, Identifier p_455691_) {
         this.textures.put(p_450834_, new RenderSetup.TextureBinding(p_455691_, () -> null));
         return this;
      }

      public RenderSetup.RenderSetupBuilder withTexture(String p_451536_, Identifier p_457755_, @Nullable Supplier<GpuSampler> p_457392_) {
         this.textures.put(p_451536_, new RenderSetup.TextureBinding(p_457755_, Suppliers.memoize(() -> p_457392_ == null ? null : p_457392_.get())));
         return this;
      }

      public RenderSetup.RenderSetupBuilder useLightmap() {
         this.useLightmap = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder useOverlay() {
         this.useOverlay = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder affectsCrumbling() {
         this.affectsCrumbling = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder sortOnUpload() {
         this.sortOnUpload = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder bufferSize(int p_461063_) {
         this.bufferSize = p_461063_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setLayeringTransform(LayeringTransform p_460097_) {
         this.layeringTransform = p_460097_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOutputTarget(OutputTarget p_461092_) {
         this.outputTarget = p_461092_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setTextureTransform(TextureTransform p_454187_) {
         this.textureTransform = p_454187_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOutline(RenderSetup.OutlineProperty p_455591_) {
         this.outlineProperty = p_455591_;
         return this;
      }

      public RenderSetup createRenderSetup() {
         return new RenderSetup(
            this.pipeline,
            this.textures,
            this.useLightmap,
            this.useOverlay,
            this.layeringTransform,
            this.outputTarget,
            this.textureTransform,
            this.outlineProperty,
            this.affectsCrumbling,
            this.sortOnUpload,
            this.bufferSize
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record TextureAndSampler(GpuTextureView textureView, GpuSampler sampler) {
   }

   @OnlyIn(Dist.CLIENT)
   record TextureBinding(Identifier location, Supplier<@Nullable GpuSampler> sampler) {
   }
}
