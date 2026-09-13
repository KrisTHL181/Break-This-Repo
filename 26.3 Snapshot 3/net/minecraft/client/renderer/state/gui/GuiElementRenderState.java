package net.minecraft.client.renderer.state.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.jspecify.annotations.Nullable;

public interface GuiElementRenderState extends ScreenArea {
   void buildVertices(final VertexConsumer vertexConsumer);

   RenderPipeline pipeline();

   TextureSetup textureSetup();

   @Nullable ScreenRectangle scissorArea();
}
