package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlShaderModule implements AutoCloseable {
   private static final int NOT_ALLOCATED = -1;
   public static final GlShaderModule INVALID_SHADER = new GlShaderModule(-1, Identifier.withDefaultNamespace("invalid"), ShaderType.VERTEX);
   private final Identifier id;
   private int shaderId;
   private final ShaderType type;

   public GlShaderModule(int p_394164_, Identifier p_453282_, ShaderType p_396671_) {
      this.id = p_453282_;
      this.shaderId = p_394164_;
      this.type = p_396671_;
   }

   @Override
   public void close() {
      if (this.shaderId == -1) {
         throw new IllegalStateException("Already closed");
      }

      RenderSystem.assertOnRenderThread();
      GlStateManager.glDeleteShader(this.shaderId);
      this.shaderId = -1;
   }

   public Identifier getId() {
      return this.id;
   }

   public int getShaderId() {
      return this.shaderId;
   }

   public String getDebugLabel() {
      return this.type.idConverter().idToFile(this.id).toString();
   }
}
