package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShadowFeatureRenderer {
   private static final RenderType SHADOW_RENDER_TYPE = RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png"));

   public void render(SubmitNodeCollection p_428628_, MultiBufferSource.BufferSource p_424089_) {
      VertexConsumer vertexconsumer = p_424089_.getBuffer(SHADOW_RENDER_TYPE);

      for (SubmitNodeStorage.ShadowSubmit submitnodestorage$shadowsubmit : p_428628_.getShadowSubmits()) {
         for (EntityRenderState.ShadowPiece entityrenderstate$shadowpiece : submitnodestorage$shadowsubmit.pieces()) {
            AABB aabb = entityrenderstate$shadowpiece.shapeBelow().bounds();
            float f = entityrenderstate$shadowpiece.relativeX() + (float)aabb.minX;
            float f1 = entityrenderstate$shadowpiece.relativeX() + (float)aabb.maxX;
            float f2 = entityrenderstate$shadowpiece.relativeY() + (float)aabb.minY;
            float f3 = entityrenderstate$shadowpiece.relativeZ() + (float)aabb.minZ;
            float f4 = entityrenderstate$shadowpiece.relativeZ() + (float)aabb.maxZ;
            float f5 = submitnodestorage$shadowsubmit.radius();
            float f6 = -f / 2.0F / f5 + 0.5F;
            float f7 = -f1 / 2.0F / f5 + 0.5F;
            float f8 = -f3 / 2.0F / f5 + 0.5F;
            float f9 = -f4 / 2.0F / f5 + 0.5F;
            int i = ARGB.white(entityrenderstate$shadowpiece.alpha());
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f, f2, f3, f6, f8);
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f, f2, f4, f6, f9);
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f1, f2, f4, f7, f9);
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f1, f2, f3, f7, f8);
         }
      }
   }

   private static void shadowVertex(
      Matrix4f p_422448_, VertexConsumer p_424638_, int p_430670_, float p_431160_, float p_425839_, float p_424173_, float p_425843_, float p_429215_
   ) {
      Vector3f vector3f = p_422448_.transformPosition(p_431160_, p_425839_, p_424173_, new Vector3f());
      p_424638_.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), p_430670_, p_425843_, p_429215_, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
   }
}
