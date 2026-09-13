package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SubmitNodeCollector extends OrderedSubmitNodeCollector {
   OrderedSubmitNodeCollector order(int var1);

   @OnlyIn(Dist.CLIENT)
   interface CustomGeometryRenderer {
      void render(PoseStack.Pose var1, VertexConsumer var2);
   }

   @OnlyIn(Dist.CLIENT)
   interface ParticleGroupRenderer {
      QuadParticleRenderState.@Nullable PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache var1);

      void render(
         QuadParticleRenderState.PreparedBuffers var1, ParticleFeatureRenderer.ParticleBufferCache var2, RenderPass var3, TextureManager var4, boolean var5
      );
   }
}
