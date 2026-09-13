package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LeashFeatureRenderer {
   private static final int LEASH_RENDER_STEPS = 24;
   private static final float LEASH_WIDTH = 0.05F;

   public void render(SubmitNodeCollection p_430591_, MultiBufferSource.BufferSource p_422464_) {
      for (SubmitNodeStorage.LeashSubmit submitnodestorage$leashsubmit : p_430591_.getLeashSubmits()) {
         renderLeash(submitnodestorage$leashsubmit.pose(), p_422464_, submitnodestorage$leashsubmit.leashState());
      }
   }

   private static void renderLeash(Matrix4f p_422982_, MultiBufferSource p_428794_, EntityRenderState.LeashState p_426797_) {
      float f = (float)(p_426797_.end.x - p_426797_.start.x);
      float f1 = (float)(p_426797_.end.y - p_426797_.start.y);
      float f2 = (float)(p_426797_.end.z - p_426797_.start.z);
      float f3 = Mth.invSqrt(f * f + f2 * f2) * 0.05F / 2.0F;
      float f4 = f2 * f3;
      float f5 = f * f3;
      p_422982_.translate((float)p_426797_.offset.x, (float)p_426797_.offset.y, (float)p_426797_.offset.z);
      VertexConsumer vertexconsumer = p_428794_.getBuffer(RenderTypes.leash());

      for (int i = 0; i <= 24; i++) {
         addVertexPair(vertexconsumer, p_422982_, f, f1, f2, 0.05F, f4, f5, i, false, p_426797_);
      }

      for (int j = 24; j >= 0; j--) {
         addVertexPair(vertexconsumer, p_422982_, f, f1, f2, 0.0F, f4, f5, j, true, p_426797_);
      }
   }

   private static void addVertexPair(
      VertexConsumer p_422928_,
      Matrix4f p_429954_,
      float p_427517_,
      float p_431671_,
      float p_422285_,
      float p_422709_,
      float p_422646_,
      float p_425836_,
      int p_428108_,
      boolean p_426059_,
      EntityRenderState.LeashState p_430731_
   ) {
      float f = p_428108_ / 24.0F;
      int i = (int)Mth.lerp(f, p_430731_.startBlockLight, p_430731_.endBlockLight);
      int j = (int)Mth.lerp(f, p_430731_.startSkyLight, p_430731_.endSkyLight);
      int k = LightTexture.pack(i, j);
      float f1 = p_428108_ % 2 == (p_426059_ ? 1 : 0) ? 0.7F : 1.0F;
      float f2 = 0.5F * f1;
      float f3 = 0.4F * f1;
      float f4 = 0.3F * f1;
      float f5 = p_427517_ * f;
      float f6;
      if (p_430731_.slack) {
         f6 = p_431671_ > 0.0F ? p_431671_ * f * f : p_431671_ - p_431671_ * (1.0F - f) * (1.0F - f);
      } else {
         f6 = p_431671_ * f;
      }

      float f7 = p_422285_ * f;
      p_422928_.addVertex(p_429954_, f5 - p_422646_, f6 + p_422709_, f7 + p_425836_).setColor(f2, f3, f4, 1.0F).setLight(k);
      p_422928_.addVertex(p_429954_, f5 + p_422646_, f6 + 0.05F - p_422709_, f7 - p_425836_).setColor(f2, f3, f4, 1.0F).setLight(k);
   }
}
