package net.minecraft.client.renderer.gizmos;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class DrawableGizmoPrimitives implements GizmoPrimitives {
   private final DrawableGizmoPrimitives.Group opaque = new DrawableGizmoPrimitives.Group(true);
   private final DrawableGizmoPrimitives.Group translucent = new DrawableGizmoPrimitives.Group(false);
   private boolean isEmpty = true;

   private DrawableGizmoPrimitives.Group getGroup(int p_452145_) {
      return ARGB.alpha(p_452145_) < 255 ? this.translucent : this.opaque;
   }

   @Override
   public void addPoint(Vec3 p_452811_, int p_451158_, float p_454233_) {
      this.getGroup(p_451158_).points.add(new DrawableGizmoPrimitives.Point(p_452811_, p_451158_, p_454233_));
      this.isEmpty = false;
   }

   @Override
   public void addLine(Vec3 p_459195_, Vec3 p_454251_, int p_452275_, float p_460061_) {
      this.getGroup(p_452275_).lines.add(new DrawableGizmoPrimitives.Line(p_459195_, p_454251_, p_452275_, p_460061_));
      this.isEmpty = false;
   }

   @Override
   public void addTriangleFan(Vec3[] p_457726_, int p_458812_) {
      this.getGroup(p_458812_).triangleFans.add(new DrawableGizmoPrimitives.TriangleFan(p_457726_, p_458812_));
      this.isEmpty = false;
   }

   @Override
   public void addQuad(Vec3 p_453709_, Vec3 p_457409_, Vec3 p_457195_, Vec3 p_452577_, int p_454746_) {
      this.getGroup(p_454746_).quads.add(new DrawableGizmoPrimitives.Quad(p_453709_, p_457409_, p_457195_, p_452577_, p_454746_));
      this.isEmpty = false;
   }

   @Override
   public void addText(Vec3 p_450470_, String p_459140_, TextGizmo.Style p_458237_) {
      this.getGroup(p_458237_.color()).texts.add(new DrawableGizmoPrimitives.Text(p_450470_, p_459140_, p_458237_));
      this.isEmpty = false;
   }

   public void render(PoseStack p_458002_, MultiBufferSource p_459724_, CameraRenderState p_453528_, Matrix4f p_453211_) {
      this.opaque.render(p_458002_, p_459724_, p_453528_, p_453211_);
      this.translucent.render(p_458002_, p_459724_, p_453528_, p_453211_);
   }

   public boolean isEmpty() {
      return this.isEmpty;
   }

   @OnlyIn(Dist.CLIENT)
   record Group(
      boolean opaque,
      List<DrawableGizmoPrimitives.Line> lines,
      List<DrawableGizmoPrimitives.Quad> quads,
      List<DrawableGizmoPrimitives.TriangleFan> triangleFans,
      List<DrawableGizmoPrimitives.Text> texts,
      List<DrawableGizmoPrimitives.Point> points
   ) {
      Group(boolean p_461012_) {
         this(p_461012_, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
      }

      public void render(PoseStack p_450270_, MultiBufferSource p_460951_, CameraRenderState p_451576_, Matrix4f p_452284_) {
         this.renderQuads(p_450270_, p_460951_, p_451576_);
         this.renderTriangleFans(p_450270_, p_460951_, p_451576_);
         this.renderLines(p_450270_, p_460951_, p_451576_, p_452284_);
         this.renderTexts(p_450270_, p_460951_, p_451576_);
         this.renderPoints(p_450270_, p_460951_, p_451576_);
      }

      private void renderTexts(PoseStack p_450248_, MultiBufferSource p_460104_, CameraRenderState p_450541_) {
         Minecraft minecraft = Minecraft.getInstance();
         Font font = minecraft.font;
         if (p_450541_.initialized) {
            double d0 = p_450541_.pos.x();
            double d1 = p_450541_.pos.y();
            double d2 = p_450541_.pos.z();

            for (DrawableGizmoPrimitives.Text drawablegizmoprimitives$text : this.texts) {
               p_450248_.pushPose();
               p_450248_.translate(
                  (float)(drawablegizmoprimitives$text.pos().x() - d0),
                  (float)(drawablegizmoprimitives$text.pos().y() - d1),
                  (float)(drawablegizmoprimitives$text.pos().z() - d2)
               );
               p_450248_.mulPose(p_450541_.orientation);
               p_450248_.scale(
                  drawablegizmoprimitives$text.style.scale() / 16.0F,
                  -drawablegizmoprimitives$text.style.scale() / 16.0F,
                  drawablegizmoprimitives$text.style.scale() / 16.0F
               );
               float f;
               if (drawablegizmoprimitives$text.style.adjustLeft().isEmpty()) {
                  f = -font.width(drawablegizmoprimitives$text.text) / 2.0F;
               } else {
                  f = (float)(-drawablegizmoprimitives$text.style.adjustLeft().getAsDouble()) / drawablegizmoprimitives$text.style.scale();
               }

               font.drawInBatch(
                  drawablegizmoprimitives$text.text,
                  f,
                  0.0F,
                  drawablegizmoprimitives$text.style.color(),
                  false,
                  p_450248_.last().pose(),
                  p_460104_,
                  Font.DisplayMode.NORMAL,
                  0,
                  15728880
               );
               p_450248_.popPose();
            }
         }
      }

      private void renderLines(PoseStack p_459303_, MultiBufferSource p_458729_, CameraRenderState p_454165_, Matrix4f p_459097_) {
         VertexConsumer vertexconsumer = p_458729_.getBuffer(this.opaque ? RenderTypes.lines() : RenderTypes.linesTranslucent());
         PoseStack.Pose posestack$pose = p_459303_.last();
         Vector4f vector4f = new Vector4f();
         Vector4f vector4f1 = new Vector4f();
         Vector4f vector4f2 = new Vector4f();
         Vector4f vector4f3 = new Vector4f();
         Vector4f vector4f4 = new Vector4f();
         double d0 = p_454165_.pos.x();
         double d1 = p_454165_.pos.y();
         double d2 = p_454165_.pos.z();

         for (DrawableGizmoPrimitives.Line drawablegizmoprimitives$line : this.lines) {
            vector4f.set(
               drawablegizmoprimitives$line.start().x() - d0, drawablegizmoprimitives$line.start().y() - d1, drawablegizmoprimitives$line.start().z() - d2, 1.0
            );
            vector4f1.set(
               drawablegizmoprimitives$line.end().x() - d0, drawablegizmoprimitives$line.end().y() - d1, drawablegizmoprimitives$line.end().z() - d2, 1.0
            );
            vector4f.mul(p_459097_, vector4f2);
            vector4f1.mul(p_459097_, vector4f3);
            boolean flag = vector4f2.z > -0.05F;
            boolean flag1 = vector4f3.z > -0.05F;
            if (!flag || !flag1) {
               if (flag || flag1) {
                  float f = vector4f3.z - vector4f2.z;
                  if (Math.abs(f) < 1.0E-9F) {
                     continue;
                  }

                  float f1 = Mth.clamp((-0.05F - vector4f2.z) / f, 0.0F, 1.0F);
                  vector4f.lerp(vector4f1, f1, vector4f4);
                  if (flag) {
                     vector4f.set(vector4f4);
                  } else {
                     vector4f1.set(vector4f4);
                  }
               }

               vertexconsumer.addVertex(posestack$pose, vector4f.x, vector4f.y, vector4f.z)
                  .setNormal(posestack$pose, vector4f1.x - vector4f.x, vector4f1.y - vector4f.y, vector4f1.z - vector4f.z)
                  .setColor(drawablegizmoprimitives$line.color())
                  .setLineWidth(drawablegizmoprimitives$line.width());
               vertexconsumer.addVertex(posestack$pose, vector4f1.x, vector4f1.y, vector4f1.z)
                  .setNormal(posestack$pose, vector4f1.x - vector4f.x, vector4f1.y - vector4f.y, vector4f1.z - vector4f.z)
                  .setColor(drawablegizmoprimitives$line.color())
                  .setLineWidth(drawablegizmoprimitives$line.width());
            }
         }
      }

      private void renderTriangleFans(PoseStack p_456182_, MultiBufferSource p_459249_, CameraRenderState p_451788_) {
         PoseStack.Pose posestack$pose = p_456182_.last();
         double d0 = p_451788_.pos.x();
         double d1 = p_451788_.pos.y();
         double d2 = p_451788_.pos.z();

         for (DrawableGizmoPrimitives.TriangleFan drawablegizmoprimitives$trianglefan : this.triangleFans) {
            VertexConsumer vertexconsumer = p_459249_.getBuffer(RenderTypes.debugTriangleFan());

            for (Vec3 vec3 : drawablegizmoprimitives$trianglefan.points()) {
               vertexconsumer.addVertex(posestack$pose, (float)(vec3.x() - d0), (float)(vec3.y() - d1), (float)(vec3.z() - d2))
                  .setColor(drawablegizmoprimitives$trianglefan.color());
            }
         }
      }

      private void renderQuads(PoseStack p_459854_, MultiBufferSource p_461091_, CameraRenderState p_450352_) {
         VertexConsumer vertexconsumer = p_461091_.getBuffer(RenderTypes.debugFilledBox());
         PoseStack.Pose posestack$pose = p_459854_.last();
         double d0 = p_450352_.pos.x();
         double d1 = p_450352_.pos.y();
         double d2 = p_450352_.pos.z();

         for (DrawableGizmoPrimitives.Quad drawablegizmoprimitives$quad : this.quads) {
            vertexconsumer.addVertex(
                  posestack$pose,
                  (float)(drawablegizmoprimitives$quad.a().x() - d0),
                  (float)(drawablegizmoprimitives$quad.a().y() - d1),
                  (float)(drawablegizmoprimitives$quad.a().z() - d2)
               )
               .setColor(drawablegizmoprimitives$quad.color());
            vertexconsumer.addVertex(
                  posestack$pose,
                  (float)(drawablegizmoprimitives$quad.b().x() - d0),
                  (float)(drawablegizmoprimitives$quad.b().y() - d1),
                  (float)(drawablegizmoprimitives$quad.b().z() - d2)
               )
               .setColor(drawablegizmoprimitives$quad.color());
            vertexconsumer.addVertex(
                  posestack$pose,
                  (float)(drawablegizmoprimitives$quad.c().x() - d0),
                  (float)(drawablegizmoprimitives$quad.c().y() - d1),
                  (float)(drawablegizmoprimitives$quad.c().z() - d2)
               )
               .setColor(drawablegizmoprimitives$quad.color());
            vertexconsumer.addVertex(
                  posestack$pose,
                  (float)(drawablegizmoprimitives$quad.d().x() - d0),
                  (float)(drawablegizmoprimitives$quad.d().y() - d1),
                  (float)(drawablegizmoprimitives$quad.d().z() - d2)
               )
               .setColor(drawablegizmoprimitives$quad.color());
         }
      }

      private void renderPoints(PoseStack p_453885_, MultiBufferSource p_460252_, CameraRenderState p_451047_) {
         VertexConsumer vertexconsumer = p_460252_.getBuffer(RenderTypes.debugPoint());
         PoseStack.Pose posestack$pose = p_453885_.last();
         double d0 = p_451047_.pos.x();
         double d1 = p_451047_.pos.y();
         double d2 = p_451047_.pos.z();

         for (DrawableGizmoPrimitives.Point drawablegizmoprimitives$point : this.points) {
            vertexconsumer.addVertex(
                  posestack$pose,
                  (float)(drawablegizmoprimitives$point.pos.x() - d0),
                  (float)(drawablegizmoprimitives$point.pos.y() - d1),
                  (float)(drawablegizmoprimitives$point.pos.z() - d2)
               )
               .setColor(drawablegizmoprimitives$point.color())
               .setLineWidth(drawablegizmoprimitives$point.size());
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Line(Vec3 start, Vec3 end, int color, float width) {
   }

   @OnlyIn(Dist.CLIENT)
   record Point(Vec3 pos, int color, float size) {
   }

   @OnlyIn(Dist.CLIENT)
   record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
   }

   @OnlyIn(Dist.CLIENT)
   record Text(Vec3 pos, String text, TextGizmo.Style style) {
   }

   @OnlyIn(Dist.CLIENT)
   record TriangleFan(Vec3[] points, int color) {
   }
}
