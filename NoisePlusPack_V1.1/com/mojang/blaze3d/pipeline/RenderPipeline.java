package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderPipeline {
   private final Identifier location;
   private final Identifier vertexShader;
   private final Identifier fragmentShader;
   private final ShaderDefines shaderDefines;
   private final List<String> samplers;
   private final List<RenderPipeline.UniformDescription> uniforms;
   private final DepthTestFunction depthTestFunction;
   private final PolygonMode polygonMode;
   private final boolean cull;
   private final LogicOp colorLogic;
   private final Optional<BlendFunction> blendFunction;
   private final boolean writeColor;
   private final boolean writeAlpha;
   private final boolean writeDepth;
   private final VertexFormat vertexFormat;
   private final VertexFormat.Mode vertexFormatMode;
   private final float depthBiasScaleFactor;
   private final float depthBiasConstant;
   private final int sortKey;
   private static int sortKeySeed;

   protected RenderPipeline(
      Identifier p_452118_,
      Identifier p_460281_,
      Identifier p_456852_,
      ShaderDefines p_395784_,
      List<String> p_393503_,
      List<RenderPipeline.UniformDescription> p_397587_,
      Optional<BlendFunction> p_391418_,
      DepthTestFunction p_391641_,
      PolygonMode p_392219_,
      boolean p_393474_,
      boolean p_392686_,
      boolean p_395911_,
      boolean p_391373_,
      LogicOp p_393123_,
      VertexFormat p_397970_,
      VertexFormat.Mode p_392120_,
      float p_397257_,
      float p_397736_,
      int p_409641_
   ) {
      this.location = p_452118_;
      this.vertexShader = p_460281_;
      this.fragmentShader = p_456852_;
      this.shaderDefines = p_395784_;
      this.samplers = p_393503_;
      this.uniforms = p_397587_;
      this.depthTestFunction = p_391641_;
      this.polygonMode = p_392219_;
      this.cull = p_393474_;
      this.blendFunction = p_391418_;
      this.writeColor = p_392686_;
      this.writeAlpha = p_395911_;
      this.writeDepth = p_391373_;
      this.colorLogic = p_393123_;
      this.vertexFormat = p_397970_;
      this.vertexFormatMode = p_392120_;
      this.depthBiasScaleFactor = p_397257_;
      this.depthBiasConstant = p_397736_;
      this.sortKey = p_409641_;
   }

   public int getSortKey() {
      return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? super.hashCode() * (sortKeySeed + 1) : this.sortKey;
   }

   public static void updateSortKeySeed() {
      sortKeySeed = Math.round(100000.0F * (float)Math.random());
   }

   @Override
   public String toString() {
      return this.location.toString();
   }

   public DepthTestFunction getDepthTestFunction() {
      return this.depthTestFunction;
   }

   public PolygonMode getPolygonMode() {
      return this.polygonMode;
   }

   public boolean isCull() {
      return this.cull;
   }

   public LogicOp getColorLogic() {
      return this.colorLogic;
   }

   public Optional<BlendFunction> getBlendFunction() {
      return this.blendFunction;
   }

   public boolean isWriteColor() {
      return this.writeColor;
   }

   public boolean isWriteAlpha() {
      return this.writeAlpha;
   }

   public boolean isWriteDepth() {
      return this.writeDepth;
   }

   public float getDepthBiasScaleFactor() {
      return this.depthBiasScaleFactor;
   }

   public float getDepthBiasConstant() {
      return this.depthBiasConstant;
   }

   public Identifier getLocation() {
      return this.location;
   }

   public VertexFormat getVertexFormat() {
      return this.vertexFormat;
   }

   public VertexFormat.Mode getVertexFormatMode() {
      return this.vertexFormatMode;
   }

   public Identifier getVertexShader() {
      return this.vertexShader;
   }

   public Identifier getFragmentShader() {
      return this.fragmentShader;
   }

   public ShaderDefines getShaderDefines() {
      return this.shaderDefines;
   }

   public List<String> getSamplers() {
      return this.samplers;
   }

   public List<RenderPipeline.UniformDescription> getUniforms() {
      return this.uniforms;
   }

   public boolean wantsDepthTexture() {
      return this.depthTestFunction != DepthTestFunction.NO_DEPTH_TEST
         || this.depthBiasConstant != 0.0F
         || this.depthBiasScaleFactor != 0.0F
         || this.writeDepth;
   }

   public static RenderPipeline.Builder builder(RenderPipeline.Snippet... p_394225_) {
      RenderPipeline.Builder renderpipeline$builder = new RenderPipeline.Builder();

      for (RenderPipeline.Snippet renderpipeline$snippet : p_394225_) {
         renderpipeline$builder.withSnippet(renderpipeline$snippet);
      }

      return renderpipeline$builder;
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public static class Builder {
      private static int nextPipelineSortKey;
      private Optional<Identifier> location = Optional.empty();
      private Optional<Identifier> fragmentShader = Optional.empty();
      private Optional<Identifier> vertexShader = Optional.empty();
      private Optional<ShaderDefines.Builder> definesBuilder = Optional.empty();
      private Optional<List<String>> samplers = Optional.empty();
      private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
      private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
      private Optional<PolygonMode> polygonMode = Optional.empty();
      private Optional<Boolean> cull = Optional.empty();
      private Optional<Boolean> writeColor = Optional.empty();
      private Optional<Boolean> writeAlpha = Optional.empty();
      private Optional<Boolean> writeDepth = Optional.empty();
      private Optional<LogicOp> colorLogic = Optional.empty();
      private Optional<BlendFunction> blendFunction = Optional.empty();
      private Optional<VertexFormat> vertexFormat = Optional.empty();
      private Optional<VertexFormat.Mode> vertexFormatMode = Optional.empty();
      private float depthBiasScaleFactor;
      private float depthBiasConstant;

      Builder() {
      }

      public RenderPipeline.Builder withLocation(String p_393717_) {
         this.location = Optional.of(Identifier.withDefaultNamespace(p_393717_));
         return this;
      }

      public RenderPipeline.Builder withLocation(Identifier p_460910_) {
         this.location = Optional.of(p_460910_);
         return this;
      }

      public RenderPipeline.Builder withFragmentShader(String p_397843_) {
         this.fragmentShader = Optional.of(Identifier.withDefaultNamespace(p_397843_));
         return this;
      }

      public RenderPipeline.Builder withFragmentShader(Identifier p_460494_) {
         this.fragmentShader = Optional.of(p_460494_);
         return this;
      }

      public RenderPipeline.Builder withVertexShader(String p_397612_) {
         this.vertexShader = Optional.of(Identifier.withDefaultNamespace(p_397612_));
         return this;
      }

      public RenderPipeline.Builder withVertexShader(Identifier p_452280_) {
         this.vertexShader = Optional.of(p_452280_);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String p_395253_) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         this.definesBuilder.get().define(p_395253_);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String p_395289_, int p_395232_) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         this.definesBuilder.get().define(p_395289_, p_395232_);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String p_391715_, float p_397590_) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         this.definesBuilder.get().define(p_391715_, p_397590_);
         return this;
      }

      public RenderPipeline.Builder withSampler(String p_392621_) {
         if (this.samplers.isEmpty()) {
            this.samplers = Optional.of(new ArrayList<>());
         }

         this.samplers.get().add(p_392621_);
         return this;
      }

      public RenderPipeline.Builder withUniform(String p_392083_, UniformType p_394752_) {
         if (this.uniforms.isEmpty()) {
            this.uniforms = Optional.of(new ArrayList<>());
         }

         if (p_394752_ == UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
         }

         this.uniforms.get().add(new RenderPipeline.UniformDescription(p_392083_, p_394752_));
         return this;
      }

      public RenderPipeline.Builder withUniform(String p_409465_, UniformType p_409341_, TextureFormat p_408238_) {
         if (this.uniforms.isEmpty()) {
            this.uniforms = Optional.of(new ArrayList<>());
         }

         if (p_409341_ != UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Only texel buffer can specify texture format");
         }

         this.uniforms.get().add(new RenderPipeline.UniformDescription(p_409465_, p_408238_));
         return this;
      }

      public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction p_391772_) {
         this.depthTestFunction = Optional.of(p_391772_);
         return this;
      }

      public RenderPipeline.Builder withPolygonMode(PolygonMode p_394810_) {
         this.polygonMode = Optional.of(p_394810_);
         return this;
      }

      public RenderPipeline.Builder withCull(boolean p_394079_) {
         this.cull = Optional.of(p_394079_);
         return this;
      }

      public RenderPipeline.Builder withBlend(BlendFunction p_397423_) {
         this.blendFunction = Optional.of(p_397423_);
         return this;
      }

      public RenderPipeline.Builder withoutBlend() {
         this.blendFunction = Optional.empty();
         return this;
      }

      public RenderPipeline.Builder withColorWrite(boolean p_393590_) {
         this.writeColor = Optional.of(p_393590_);
         this.writeAlpha = Optional.of(p_393590_);
         return this;
      }

      public RenderPipeline.Builder withColorWrite(boolean p_394967_, boolean p_397968_) {
         this.writeColor = Optional.of(p_394967_);
         this.writeAlpha = Optional.of(p_397968_);
         return this;
      }

      public RenderPipeline.Builder withDepthWrite(boolean p_391289_) {
         this.writeDepth = Optional.of(p_391289_);
         return this;
      }

      @Deprecated
      public RenderPipeline.Builder withColorLogic(LogicOp p_396738_) {
         this.colorLogic = Optional.of(p_396738_);
         return this;
      }

      public RenderPipeline.Builder withVertexFormat(VertexFormat p_392486_, VertexFormat.Mode p_392679_) {
         this.vertexFormat = Optional.of(p_392486_);
         this.vertexFormatMode = Optional.of(p_392679_);
         return this;
      }

      public RenderPipeline.Builder withDepthBias(float p_397914_, float p_394987_) {
         this.depthBiasScaleFactor = p_397914_;
         this.depthBiasConstant = p_394987_;
         return this;
      }

      void withSnippet(RenderPipeline.Snippet p_395487_) {
         if (p_395487_.vertexShader.isPresent()) {
            this.vertexShader = p_395487_.vertexShader;
         }

         if (p_395487_.fragmentShader.isPresent()) {
            this.fragmentShader = p_395487_.fragmentShader;
         }

         if (p_395487_.shaderDefines.isPresent()) {
            if (this.definesBuilder.isEmpty()) {
               this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            ShaderDefines shaderdefines = p_395487_.shaderDefines.get();

            for (Entry<String, String> entry : shaderdefines.values().entrySet()) {
               this.definesBuilder.get().define(entry.getKey(), entry.getValue());
            }

            for (String s : shaderdefines.flags()) {
               this.definesBuilder.get().define(s);
            }
         }

         p_395487_.samplers.ifPresent(p_396787_ -> {
            if (this.samplers.isPresent()) {
               this.samplers.get().addAll((Collection<? extends String>)p_396787_);
            } else {
               this.samplers = Optional.of(new ArrayList<>((Collection<? extends String>)p_396787_));
            }
         });
         p_395487_.uniforms.ifPresent(p_393176_ -> {
            if (this.uniforms.isPresent()) {
               this.uniforms.get().addAll((Collection<? extends RenderPipeline.UniformDescription>)p_393176_);
            } else {
               this.uniforms = Optional.of(new ArrayList<>((Collection<? extends RenderPipeline.UniformDescription>)p_393176_));
            }
         });
         if (p_395487_.depthTestFunction.isPresent()) {
            this.depthTestFunction = p_395487_.depthTestFunction;
         }

         if (p_395487_.cull.isPresent()) {
            this.cull = p_395487_.cull;
         }

         if (p_395487_.writeColor.isPresent()) {
            this.writeColor = p_395487_.writeColor;
         }

         if (p_395487_.writeAlpha.isPresent()) {
            this.writeAlpha = p_395487_.writeAlpha;
         }

         if (p_395487_.writeDepth.isPresent()) {
            this.writeDepth = p_395487_.writeDepth;
         }

         if (p_395487_.colorLogic.isPresent()) {
            this.colorLogic = p_395487_.colorLogic;
         }

         if (p_395487_.blendFunction.isPresent()) {
            this.blendFunction = p_395487_.blendFunction;
         }

         if (p_395487_.vertexFormat.isPresent()) {
            this.vertexFormat = p_395487_.vertexFormat;
         }

         if (p_395487_.vertexFormatMode.isPresent()) {
            this.vertexFormatMode = p_395487_.vertexFormatMode;
         }
      }

      public RenderPipeline.Snippet buildSnippet() {
         return new RenderPipeline.Snippet(
            this.vertexShader,
            this.fragmentShader,
            this.definesBuilder.map(ShaderDefines.Builder::build),
            this.samplers.map(Collections::unmodifiableList),
            this.uniforms.map(Collections::unmodifiableList),
            this.blendFunction,
            this.depthTestFunction,
            this.polygonMode,
            this.cull,
            this.writeColor,
            this.writeAlpha,
            this.writeDepth,
            this.colorLogic,
            this.vertexFormat,
            this.vertexFormatMode
         );
      }

      public RenderPipeline build() {
         if (this.location.isEmpty()) {
            throw new IllegalStateException("Missing location");
         } else if (this.vertexShader.isEmpty()) {
            throw new IllegalStateException("Missing vertex shader");
         } else if (this.fragmentShader.isEmpty()) {
            throw new IllegalStateException("Missing fragment shader");
         } else if (this.vertexFormat.isEmpty()) {
            throw new IllegalStateException("Missing vertex buffer format");
         } else if (this.vertexFormatMode.isEmpty()) {
            throw new IllegalStateException("Missing vertex mode");
         } else {
            return new RenderPipeline(
               this.location.get(),
               this.vertexShader.get(),
               this.fragmentShader.get(),
               this.definesBuilder.orElse(ShaderDefines.builder()).build(),
               List.copyOf(this.samplers.orElse(new ArrayList<>())),
               this.uniforms.orElse(Collections.emptyList()),
               this.blendFunction,
               this.depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST),
               this.polygonMode.orElse(PolygonMode.FILL),
               this.cull.orElse(true),
               this.writeColor.orElse(true),
               this.writeAlpha.orElse(true),
               this.writeDepth.orElse(true),
               this.colorLogic.orElse(LogicOp.NONE),
               this.vertexFormat.get(),
               this.vertexFormatMode.get(),
               this.depthBiasScaleFactor,
               this.depthBiasConstant,
               nextPipelineSortKey++
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public record Snippet(
      Optional<Identifier> vertexShader,
      Optional<Identifier> fragmentShader,
      Optional<ShaderDefines> shaderDefines,
      Optional<List<String>> samplers,
      Optional<List<RenderPipeline.UniformDescription>> uniforms,
      Optional<BlendFunction> blendFunction,
      Optional<DepthTestFunction> depthTestFunction,
      Optional<PolygonMode> polygonMode,
      Optional<Boolean> cull,
      Optional<Boolean> writeColor,
      Optional<Boolean> writeAlpha,
      Optional<Boolean> writeDepth,
      Optional<LogicOp> colorLogic,
      Optional<VertexFormat> vertexFormat,
      Optional<VertexFormat.Mode> vertexFormatMode
   ) {
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public record UniformDescription(String name, UniformType type, @Nullable TextureFormat textureFormat) {
      public UniformDescription(String p_396501_, UniformType p_396078_) {
         this(p_396501_, p_396078_, null);
         if (p_396078_ == UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Texel buffer needs a texture format");
         }
      }

      public UniformDescription(String p_410419_, TextureFormat p_406895_) {
         this(p_410419_, UniformType.TEXEL_BUFFER, p_406895_);
      }
   }
}
