package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class GizmoFeatureRenderer extends RenderTypeFeatureRenderer<GizmoFeatureRenderer.Submit> {
   public static final FeatureRendererType<GizmoFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Gizmo");
   private final PoseStack poseStack = new PoseStack();

   @Override
   protected void buildGroup(final FeatureFrameContext context, final List<GizmoFeatureRenderer.Submit> submits) {
      Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrixCopy();

      for (GizmoFeatureRenderer.Submit submit : submits) {
         CameraRenderState camera = submit.camera;
         this.buildQuads(submit.group.quads(), camera);
         this.buildTriangleFans(submit.group.triangleFans(), camera);
         this.buildLines(submit.group.lines(), camera, modelViewMatrix, submit.group.opaque());
         this.buildTexts(submit.group.texts(), camera, context.font());
         this.buildPoints(submit.group.points(), camera);
      }
   }

   private void buildTexts(final List<DrawableGizmoPrimitives.Text> texts, final CameraRenderState camera, final Font font) {
      if (!texts.isEmpty() && camera.initialized) {
         double camX = camera.pos.x();
         double camY = camera.pos.y();
         double camZ = camera.pos.z();

         for (DrawableGizmoPrimitives.Text text : texts) {
            TextGizmo.Style style = text.style();
            this.poseStack.pushPose();
            this.poseStack.translate((float)(text.pos().x() - camX), (float)(text.pos().y() - camY), (float)(text.pos().z() - camZ));
            this.poseStack.mulPose(camera.orientation);
            this.poseStack.scale(style.scale() / 16.0F, -style.scale() / 16.0F, style.scale() / 16.0F);
            float fontX;
            if (style.adjustLeft().isEmpty()) {
               fontX = -font.width(text.text()) / 2.0F;
            } else {
               fontX = (float)(-style.adjustLeft().getAsDouble()) / style.scale();
            }

            final Matrix4f pose = this.poseStack.last().pose();
            Font.PreparedText preparedText = font.prepareText(text.text(), fontX, 0.0F, style.color(), false, 0);
            preparedText.visit(new Font.GlyphVisitor() {
               @Override
               public void acceptRenderable(final TextRenderable renderable) {
                  VertexConsumer buffer = GizmoFeatureRenderer.this.getVertexBuilder(renderable.renderType(Font.DisplayMode.NORMAL));
                  renderable.render(pose, buffer, 15728880, false);
               }
            });
            this.poseStack.popPose();
         }
      }
   }

   private void buildLines(
      final List<DrawableGizmoPrimitives.Line> lines, final CameraRenderState camera, final Matrix4fc modelViewMatrix, final boolean opaque
   ) {
      if (!lines.isEmpty()) {
         VertexConsumer builder = this.getVertexBuilder(opaque ? RenderTypes.lines() : RenderTypes.linesTranslucent());
         PoseStack.Pose pose = this.poseStack.last();
         Vector4f start = new Vector4f();
         Vector4f end = new Vector4f();
         Vector4f startViewSpace = new Vector4f();
         Vector4f endViewSpace = new Vector4f();
         Vector4f intersectionInWorld = new Vector4f();
         double camX = camera.pos.x();
         double camY = camera.pos.y();
         double camZ = camera.pos.z();

         for (DrawableGizmoPrimitives.Line line : lines) {
            start.set(line.start().x() - camX, line.start().y() - camY, line.start().z() - camZ, 1.0);
            end.set(line.end().x() - camX, line.end().y() - camY, line.end().z() - camZ, 1.0);
            start.mul(modelViewMatrix, startViewSpace);
            end.mul(modelViewMatrix, endViewSpace);
            boolean startIsBehindCamera = startViewSpace.z > -0.05F;
            boolean endIsBehindCamera = endViewSpace.z > -0.05F;
            if (!startIsBehindCamera || !endIsBehindCamera) {
               if (startIsBehindCamera || endIsBehindCamera) {
                  float denom = endViewSpace.z - startViewSpace.z;
                  if (Math.abs(denom) < 1.0E-9F) {
                     continue;
                  }

                  float intersection = Mth.clamp((-0.05F - startViewSpace.z) / denom, 0.0F, 1.0F);
                  start.lerp(end, intersection, intersectionInWorld);
                  if (startIsBehindCamera) {
                     start.set(intersectionInWorld);
                  } else {
                     end.set(intersectionInWorld);
                  }
               }

               builder.addVertex(pose, start.x, start.y, start.z)
                  .setNormal(pose, end.x - start.x, end.y - start.y, end.z - start.z)
                  .setColor(line.color())
                  .setLineWidth(line.width());
               builder.addVertex(pose, end.x, end.y, end.z)
                  .setNormal(pose, end.x - start.x, end.y - start.y, end.z - start.z)
                  .setColor(line.color())
                  .setLineWidth(line.width());
            }
         }
      }
   }

   private void buildTriangleFans(final List<DrawableGizmoPrimitives.TriangleFan> triangleFans, final CameraRenderState camera) {
      if (!triangleFans.isEmpty()) {
         PoseStack.Pose pose = this.poseStack.last();
         double camX = camera.pos.x();
         double camY = camera.pos.y();
         double camZ = camera.pos.z();

         for (DrawableGizmoPrimitives.TriangleFan triangleFan : triangleFans) {
            VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugTriangleFan());

            for (Vec3 point : triangleFan.points()) {
               builder.addVertex(pose, (float)(point.x() - camX), (float)(point.y() - camY), (float)(point.z() - camZ)).setColor(triangleFan.color());
            }
         }
      }
   }

   private void buildQuads(final List<DrawableGizmoPrimitives.Quad> quads, final CameraRenderState camera) {
      if (!quads.isEmpty()) {
         VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugFilledBox());
         PoseStack.Pose pose = this.poseStack.last();
         double camX = camera.pos.x();
         double camY = camera.pos.y();
         double camZ = camera.pos.z();

         for (DrawableGizmoPrimitives.Quad quad : quads) {
            builder.addVertex(pose, (float)(quad.a().x() - camX), (float)(quad.a().y() - camY), (float)(quad.a().z() - camZ)).setColor(quad.color());
            builder.addVertex(pose, (float)(quad.b().x() - camX), (float)(quad.b().y() - camY), (float)(quad.b().z() - camZ)).setColor(quad.color());
            builder.addVertex(pose, (float)(quad.c().x() - camX), (float)(quad.c().y() - camY), (float)(quad.c().z() - camZ)).setColor(quad.color());
            builder.addVertex(pose, (float)(quad.d().x() - camX), (float)(quad.d().y() - camY), (float)(quad.d().z() - camZ)).setColor(quad.color());
         }
      }
   }

   private void buildPoints(final List<DrawableGizmoPrimitives.Point> points, final CameraRenderState camera) {
      if (!points.isEmpty()) {
         VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugPoint());
         PoseStack.Pose pose = this.poseStack.last();
         double camX = camera.pos.x();
         double camY = camera.pos.y();
         double camZ = camera.pos.z();

         for (DrawableGizmoPrimitives.Point point : points) {
            Vec3 pos = point.pos();
            builder.addVertex(pose, (float)(pos.x() - camX), (float)(pos.y() - camY), (float)(pos.z() - camZ))
               .setColor(point.color())
               .setLineWidth(point.size());
         }
      }
   }

   public record Submit(DrawableGizmoPrimitives.Group group, CameraRenderState camera) implements SubmitNode {
      @Override
      public FeatureRendererType<GizmoFeatureRenderer.Submit> featureType() {
         return GizmoFeatureRenderer.TYPE;
      }
   }
}
