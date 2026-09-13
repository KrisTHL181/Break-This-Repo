package net.minecraft.client.renderer.rendertype;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class RenderSetup {
   final RenderPipeline pipeline;
   final @Nullable OitPipelineSet oitPipelineSet;
   final @Nullable RenderPipeline opaquePartsPipeline;
   final Map<String, RenderSetup.TextureBinding> textures;
   final TextureTransform textureTransform;
   final RenderSetup.OutlineProperty outlineProperty;
   final boolean useLightmap;
   final boolean useOverlay;
   final boolean affectsCrumbling;
   final boolean sortOnUpload;
   final LayeringTransform layeringTransform;
   final boolean forceSolidModelPhase;

   private RenderSetup(
      final RenderPipeline pipeline,
      final @Nullable OitPipelineSet oitPipelineSet,
      final @Nullable RenderPipeline opaquePartsPipeline,
      final Map<String, RenderSetup.TextureBinding> textures,
      final boolean useLightmap,
      final boolean useOverlay,
      final LayeringTransform layeringTransform,
      final TextureTransform textureTransform,
      final RenderSetup.OutlineProperty outlineProperty,
      final boolean affectsCrumbling,
      final boolean sortOnUpload,
      final boolean forceSolidModelPhase
   ) {
      this.pipeline = pipeline;
      this.oitPipelineSet = oitPipelineSet;
      this.opaquePartsPipeline = opaquePartsPipeline;
      this.textures = textures;
      this.textureTransform = textureTransform;
      this.useLightmap = useLightmap;
      this.useOverlay = useOverlay;
      this.outlineProperty = outlineProperty;
      this.layeringTransform = layeringTransform;
      this.affectsCrumbling = affectsCrumbling;
      this.sortOnUpload = sortOnUpload;
      this.forceSolidModelPhase = forceSolidModelPhase;
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

   public static RenderSetup.RenderSetupBuilder builder(final RenderPipeline pipeline) {
      return new RenderSetup.RenderSetupBuilder(pipeline);
   }

   public List<PreparedRenderType.Texture> prepareTextures(
      final TextureManager textureManager, final SamplerCache samplerCache, final GpuTextureView overlayTexture, final GpuTextureView lightmapTexture
   ) {
      if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap) {
         return List.of();
      }

      Builder<PreparedRenderType.Texture> textures = ImmutableList.builderWithExpectedSize(this.textures.size() + 2);
      if (this.useOverlay) {
         textures.add(new PreparedRenderType.Texture("Sampler1", overlayTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)));
      }

      if (this.useLightmap) {
         textures.add(new PreparedRenderType.Texture("Sampler2", lightmapTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)));
      }

      for (Entry<String, RenderSetup.TextureBinding> entry : this.textures.entrySet()) {
         AbstractTexture texture = textureManager.getTexture(entry.getValue().location);
         GpuSampler samplerOverride = entry.getValue().sampler().get();
         textures.add(
            new PreparedRenderType.Texture(entry.getKey(), texture.getTextureView(), samplerOverride != null ? samplerOverride : texture.getSampler())
         );
      }

      return textures.build();
   }

   public enum OutlineProperty {
      NONE("none"),
      IS_OUTLINE("is_outline"),
      AFFECTS_OUTLINE("affects_outline");

      private final String name;

      OutlineProperty(final String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   public static class RenderSetupBuilder {
      private final RenderPipeline pipeline;
      private @Nullable OitPipelineSet oitPipelineSet;
      private @Nullable RenderPipeline opaquePartsPipeline;
      private boolean useLightmap = false;
      private boolean useOverlay = false;
      private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
      private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
      private @Nullable OutputTarget oitOutputTarget;
      private TextureTransform textureTransform = TextureTransform.DEFAULT_TEXTURING;
      private boolean affectsCrumbling = false;
      private boolean sortOnUpload = false;
      private RenderSetup.OutlineProperty outlineProperty = RenderSetup.OutlineProperty.NONE;
      private final Map<String, RenderSetup.TextureBinding> textures = new HashMap<>();
      private boolean forceSolidModelPhase;

      private RenderSetupBuilder(final RenderPipeline pipeline) {
         this.pipeline = pipeline;
      }

      public RenderSetup.RenderSetupBuilder withTexture(final String name, final Identifier texture) {
         this.textures.put(name, new RenderSetup.TextureBinding(texture, () -> null));
         return this;
      }

      public RenderSetup.RenderSetupBuilder withTexture(final String name, final Identifier texture, final @Nullable Supplier<GpuSampler> sampler) {
         this.textures.put(name, new RenderSetup.TextureBinding(texture, Suppliers.memoize(() -> sampler == null ? null : sampler.get())));
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

      public RenderSetup.RenderSetupBuilder setLayeringTransform(final LayeringTransform layeringTransform) {
         this.layeringTransform = layeringTransform;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setTextureTransform(final TextureTransform textureTransform) {
         this.textureTransform = textureTransform;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOutline(final RenderSetup.OutlineProperty outlineProperty) {
         this.outlineProperty = outlineProperty;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOitPipelines(final OitPipelineSet oitPipelineSet) {
         this.oitPipelineSet = oitPipelineSet;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOpaquePartsPipeline(final RenderPipeline opaquePartsPipeline) {
         this.opaquePartsPipeline = opaquePartsPipeline;
         return this;
      }

      public RenderSetup.RenderSetupBuilder withForcedSolidModelPhase() {
         this.forceSolidModelPhase = true;
         return this;
      }

      public RenderSetup createRenderSetup() {
         return new RenderSetup(
            this.pipeline,
            this.oitPipelineSet,
            this.opaquePartsPipeline,
            this.textures,
            this.useLightmap,
            this.useOverlay,
            this.layeringTransform,
            this.textureTransform,
            this.outlineProperty,
            this.affectsCrumbling,
            this.sortOnUpload,
            this.forceSolidModelPhase
         );
      }
   }

   record TextureBinding(Identifier location, Supplier<@Nullable GpuSampler> sampler) {
   }
}
