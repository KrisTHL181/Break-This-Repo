package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GlyphRenderState(Matrix3x2fc pose, TextRenderable renderable, @Nullable ScreenRectangle scissorArea) implements GuiElementRenderState {
   @Override
   public void buildVertices(VertexConsumer p_409889_) {
      this.renderable.render(new Matrix4f().mul(this.pose), p_409889_, 15728880, true);
   }

   @Override
   public RenderPipeline pipeline() {
      return this.renderable.guiPipeline();
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.singleTextureWithLightmap(this.renderable.textureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
   }

   @Override
   public @Nullable ScreenRectangle bounds() {
      return null;
   }
}
