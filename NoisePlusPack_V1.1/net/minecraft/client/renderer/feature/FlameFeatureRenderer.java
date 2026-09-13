package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class FlameFeatureRenderer {
   public void render(SubmitNodeCollection p_426889_, MultiBufferSource.BufferSource p_430500_, AtlasManager p_422974_) {
      for (SubmitNodeStorage.FlameSubmit submitnodestorage$flamesubmit : p_426889_.getFlameSubmits()) {
         this.renderFlame(
            submitnodestorage$flamesubmit.pose(),
            p_430500_,
            submitnodestorage$flamesubmit.entityRenderState(),
            submitnodestorage$flamesubmit.rotation(),
            p_422974_
         );
      }
   }

   private void renderFlame(PoseStack.Pose p_423210_, MultiBufferSource p_428039_, EntityRenderState p_427452_, Quaternionf p_431746_, AtlasManager p_428875_) {
      TextureAtlasSprite textureatlassprite = p_428875_.get(ModelBakery.FIRE_0);
      TextureAtlasSprite textureatlassprite1 = p_428875_.get(ModelBakery.FIRE_1);
      float f = p_427452_.boundingBoxWidth * 1.4F;
      p_423210_.scale(f, f, f);
      float f1 = 0.5F;
      float f2 = 0.0F;
      float f3 = p_427452_.boundingBoxHeight / f;
      float f4 = 0.0F;
      p_423210_.rotate(p_431746_);
      p_423210_.translate(0.0F, 0.0F, 0.3F - (int)f3 * 0.02F);
      float f5 = 0.0F;
      int i = 0;
      VertexConsumer vertexconsumer = p_428039_.getBuffer(Sheets.cutoutBlockSheet());

      while (f3 > 0.0F) {
         TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
         float f6 = textureatlassprite2.getU0();
         float f7 = textureatlassprite2.getV0();
         float f8 = textureatlassprite2.getU1();
         float f9 = textureatlassprite2.getV1();
         if (i / 2 % 2 == 0) {
            float f10 = f8;
            f8 = f6;
            f6 = f10;
         }

         fireVertex(p_423210_, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f8, f9);
         fireVertex(p_423210_, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f6, f9);
         fireVertex(p_423210_, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f6, f7);
         fireVertex(p_423210_, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f8, f7);
         f3 -= 0.45F;
         f4 -= 0.45F;
         f1 *= 0.9F;
         f5 -= 0.03F;
         i++;
      }
   }

   private static void fireVertex(
      PoseStack.Pose p_431295_, VertexConsumer p_426294_, float p_427057_, float p_430429_, float p_426555_, float p_428750_, float p_424333_
   ) {
      p_426294_.addVertex(p_431295_, p_427057_, p_430429_, p_426555_)
         .setColor(-1)
         .setUv(p_428750_, p_424333_)
         .setUv1(0, 10)
         .setLight(240)
         .setNormal(p_431295_, 0.0F, 1.0F, 0.0F);
   }
}
