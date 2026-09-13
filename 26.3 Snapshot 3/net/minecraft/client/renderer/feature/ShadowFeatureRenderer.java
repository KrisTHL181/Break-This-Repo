package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public class ShadowFeatureRenderer extends RenderTypeFeatureRenderer<ShadowFeatureRenderer.Submit> {
   public static final FeatureRendererType<ShadowFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Shadow");
   private static final RenderType SHADOW_RENDER_TYPE = RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png"));

   @Override
   protected void buildGroup(final FeatureFrameContext context, final List<ShadowFeatureRenderer.Submit> submits) {
      VertexConsumer builder = this.getVertexBuilder(SHADOW_RENDER_TYPE);

      for (ShadowFeatureRenderer.Submit submit : submits) {
         this.prepare(submit, builder);
      }
   }

   private void prepare(final ShadowFeatureRenderer.Submit submit, final VertexConsumer builder) {
      for (EntityRenderState.ShadowPiece piece : submit.pieces()) {
         AABB aabb = piece.shapeBelow().bounds();
         float x01 = piece.relativeX() + (float)aabb.minX;
         float x11 = piece.relativeX() + (float)aabb.maxX;
         float y01 = piece.relativeY() + (float)aabb.minY;
         float z01 = piece.relativeZ() + (float)aabb.minZ;
         float z11 = piece.relativeZ() + (float)aabb.maxZ;
         float radius = submit.radius();
         float u0 = -x01 / 2.0F / radius + 0.5F;
         float u1 = -x11 / 2.0F / radius + 0.5F;
         float v0 = -z01 / 2.0F / radius + 0.5F;
         float v1 = -z11 / 2.0F / radius + 0.5F;
         int color = ARGB.white(piece.alpha());
         shadowVertex(submit.pose(), builder, color, x01, y01, z01, u0, v0);
         shadowVertex(submit.pose(), builder, color, x01, y01, z11, u0, v1);
         shadowVertex(submit.pose(), builder, color, x11, y01, z11, u1, v1);
         shadowVertex(submit.pose(), builder, color, x11, y01, z01, u1, v0);
      }
   }

   private static void shadowVertex(
      final Matrix4fc pose, final VertexConsumer buffer, final int color, final float x, final float y, final float z, final float u, final float v
   ) {
      Vector3f position = pose.transformPosition(x, y, z, new Vector3f());
      buffer.addVertex(position.x(), position.y(), position.z(), color, u, v, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
   }

   public record Submit(Matrix4fc pose, float radius, List<EntityRenderState.ShadowPiece> pieces) implements SubmitNode {
      @Override
      public FeatureRendererType<ShadowFeatureRenderer.Submit> featureType() {
         return ShadowFeatureRenderer.TYPE;
      }
   }
}
