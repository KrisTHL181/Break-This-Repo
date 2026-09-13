package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ModelPart {
   public static final float DEFAULT_SCALE = 1.0F;
   public float x;
   public float y;
   public float z;
   public float xRot;
   public float yRot;
   public float zRot;
   public float xScale = 1.0F;
   public float yScale = 1.0F;
   public float zScale = 1.0F;
   public boolean visible = true;
   public boolean skipDraw;
   private final List<ModelPart.Cube> cubes;
   private final Map<String, ModelPart> children;
   private PartPose initialPose = PartPose.ZERO;

   public ModelPart(List<ModelPart.Cube> p_171306_, Map<String, ModelPart> p_171307_) {
      this.cubes = p_171306_;
      this.children = p_171307_;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose p_233561_) {
      this.initialPose = p_233561_;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose p_171323_) {
      this.x = p_171323_.x();
      this.y = p_171323_.y();
      this.z = p_171323_.z();
      this.xRot = p_171323_.xRot();
      this.yRot = p_171323_.yRot();
      this.zRot = p_171323_.zRot();
      this.xScale = p_171323_.xScale();
      this.yScale = p_171323_.yScale();
      this.zScale = p_171323_.zScale();
   }

   public boolean hasChild(String p_233563_) {
      return this.children.containsKey(p_233563_);
   }

   public ModelPart getChild(String p_171325_) {
      ModelPart modelpart = this.children.get(p_171325_);
      if (modelpart == null) {
         throw new NoSuchElementException("Can't find part " + p_171325_);
      } else {
         return modelpart;
      }
   }

   public void setPos(float p_104228_, float p_104229_, float p_104230_) {
      this.x = p_104228_;
      this.y = p_104229_;
      this.z = p_104230_;
   }

   public void setRotation(float p_171328_, float p_171329_, float p_171330_) {
      this.xRot = p_171328_;
      this.yRot = p_171329_;
      this.zRot = p_171330_;
   }

   public void render(PoseStack p_104302_, VertexConsumer p_104303_, int p_104304_, int p_104305_) {
      this.render(p_104302_, p_104303_, p_104304_, p_104305_, -1);
   }

   public void render(PoseStack p_104307_, VertexConsumer p_104308_, int p_104309_, int p_104310_, int p_343158_) {
      if (this.visible && (!this.cubes.isEmpty() || !this.children.isEmpty())) {
         p_104307_.pushPose();
         this.translateAndRotate(p_104307_);
         if (!this.skipDraw) {
            this.compile(p_104307_.last(), p_104308_, p_104309_, p_104310_, p_343158_);
         }

         for (ModelPart modelpart : this.children.values()) {
            modelpart.render(p_104307_, p_104308_, p_104309_, p_104310_, p_343158_);
         }

         p_104307_.popPose();
      }
   }

   public void rotateBy(Quaternionf p_365235_) {
      Matrix3f matrix3f = new Matrix3f().rotationZYX(this.zRot, this.yRot, this.xRot);
      Matrix3f matrix3f1 = matrix3f.rotate(p_365235_);
      Vector3f vector3f = matrix3f1.getEulerAnglesZYX(new Vector3f());
      this.setRotation(vector3f.x, vector3f.y, vector3f.z);
   }

   public void getExtentsForGui(PoseStack p_406230_, Consumer<Vector3fc> p_455205_) {
      this.visit(p_406230_, (p_404875_, p_404876_, p_404877_, p_404878_) -> {
         for (ModelPart.Polygon modelpart$polygon : p_404878_.polygons) {
            for (ModelPart.Vertex modelpart$vertex : modelpart$polygon.vertices()) {
               float f = modelpart$vertex.worldX();
               float f1 = modelpart$vertex.worldY();
               float f2 = modelpart$vertex.worldZ();
               Vector3f vector3f = p_404875_.pose().transformPosition(f, f1, f2, new Vector3f());
               p_455205_.accept(vector3f);
            }
         }
      });
   }

   public void visit(PoseStack p_171310_, ModelPart.Visitor p_171311_) {
      this.visit(p_171310_, p_171311_, "");
   }

   private void visit(PoseStack p_171313_, ModelPart.Visitor p_171314_, String p_171315_) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         p_171313_.pushPose();
         this.translateAndRotate(p_171313_);
         PoseStack.Pose posestack$pose = p_171313_.last();

         for (int i = 0; i < this.cubes.size(); i++) {
            p_171314_.visit(posestack$pose, p_171315_, i, this.cubes.get(i));
         }

         String s = p_171315_ + "/";
         this.children.forEach((p_171320_, p_171321_) -> p_171321_.visit(p_171313_, p_171314_, s + p_171320_));
         p_171313_.popPose();
      }
   }

   public void translateAndRotate(PoseStack p_104300_) {
      p_104300_.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         p_104300_.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         p_104300_.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   private void compile(PoseStack.Pose p_104291_, VertexConsumer p_104292_, int p_104293_, int p_104294_, int p_343687_) {
      for (ModelPart.Cube modelpart$cube : this.cubes) {
         modelpart$cube.compile(p_104291_, p_104292_, p_104293_, p_104294_, p_343687_);
      }
   }

   public ModelPart.Cube getRandomCube(RandomSource p_233559_) {
      return this.cubes.get(p_233559_.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f p_253873_) {
      this.x = this.x + p_253873_.x();
      this.y = this.y + p_253873_.y();
      this.z = this.z + p_253873_.z();
   }

   public void offsetRotation(Vector3f p_253983_) {
      this.xRot = this.xRot + p_253983_.x();
      this.yRot = this.yRot + p_253983_.y();
      this.zRot = this.zRot + p_253983_.z();
   }

   public void offsetScale(Vector3f p_253957_) {
      this.xScale = this.xScale + p_253957_.x();
      this.yScale = this.yScale + p_253957_.y();
      this.zScale = this.zScale + p_253957_.z();
   }

   public List<ModelPart> getAllParts() {
      List<ModelPart> list = new ArrayList<>();
      list.add(this);
      this.addAllChildren((p_404880_, p_404881_) -> list.add(p_404881_));
      return List.copyOf(list);
   }

   public Function<String, @Nullable ModelPart> createPartLookup() {
      Map<String, ModelPart> map = new HashMap<>();
      map.put("root", this);
      this.addAllChildren(map::putIfAbsent);
      return map::get;
   }

   private void addAllChildren(BiConsumer<String, ModelPart> p_409475_) {
      for (Entry<String, ModelPart> entry : this.children.entrySet()) {
         p_409475_.accept(entry.getKey(), entry.getValue());
      }

      for (ModelPart modelpart : this.children.values()) {
         modelpart.addAllChildren(p_409475_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Cube {
      public final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(
         int p_273701_,
         int p_273034_,
         float p_272824_,
         float p_273777_,
         float p_273748_,
         float p_273722_,
         float p_273763_,
         float p_272823_,
         float p_272945_,
         float p_272790_,
         float p_272870_,
         boolean p_273589_,
         float p_273591_,
         float p_273313_,
         Set<Direction> p_273291_
      ) {
         this.minX = p_272824_;
         this.minY = p_273777_;
         this.minZ = p_273748_;
         this.maxX = p_272824_ + p_273722_;
         this.maxY = p_273777_ + p_273763_;
         this.maxZ = p_273748_ + p_272823_;
         this.polygons = new ModelPart.Polygon[p_273291_.size()];
         float f = p_272824_ + p_273722_;
         float f1 = p_273777_ + p_273763_;
         float f2 = p_273748_ + p_272823_;
         p_272824_ -= p_272945_;
         p_273777_ -= p_272790_;
         p_273748_ -= p_272870_;
         f += p_272945_;
         f1 += p_272790_;
         f2 += p_272870_;
         if (p_273589_) {
            float f3 = f;
            f = p_272824_;
            p_272824_ = f3;
         }

         ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(p_272824_, p_273777_, p_273748_, 0.0F, 0.0F);
         ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, p_273777_, p_273748_, 0.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, p_273748_, 8.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(p_272824_, f1, p_273748_, 8.0F, 0.0F);
         ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(p_272824_, p_273777_, f2, 0.0F, 0.0F);
         ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, p_273777_, f2, 0.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(p_272824_, f1, f2, 8.0F, 0.0F);
         float f4 = p_273701_;
         float f5 = p_273701_ + p_272823_;
         float f6 = p_273701_ + p_272823_ + p_273722_;
         float f7 = p_273701_ + p_272823_ + p_273722_ + p_273722_;
         float f8 = p_273701_ + p_272823_ + p_273722_ + p_272823_;
         float f9 = p_273701_ + p_272823_ + p_273722_ + p_272823_ + p_273722_;
         float f10 = p_273034_;
         float f11 = p_273034_ + p_272823_;
         float f12 = p_273034_ + p_272823_ + p_273763_;
         int i = 0;
         if (p_273291_.contains(Direction.DOWN)) {
            this.polygons[i++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex},
               f5,
               f10,
               f6,
               f11,
               p_273591_,
               p_273313_,
               p_273589_,
               Direction.DOWN
            );
         }

         if (p_273291_.contains(Direction.UP)) {
            this.polygons[i++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5},
               f6,
               f11,
               f7,
               f10,
               p_273591_,
               p_273313_,
               p_273589_,
               Direction.UP
            );
         }

         if (p_273291_.contains(Direction.WEST)) {
            this.polygons[i++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2},
               f4,
               f11,
               f5,
               f12,
               p_273591_,
               p_273313_,
               p_273589_,
               Direction.WEST
            );
         }

         if (p_273291_.contains(Direction.NORTH)) {
            this.polygons[i++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1},
               f5,
               f11,
               f6,
               f12,
               p_273591_,
               p_273313_,
               p_273589_,
               Direction.NORTH
            );
         }

         if (p_273291_.contains(Direction.EAST)) {
            this.polygons[i++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5},
               f6,
               f11,
               f8,
               f12,
               p_273591_,
               p_273313_,
               p_273589_,
               Direction.EAST
            );
         }

         if (p_273291_.contains(Direction.SOUTH)) {
            this.polygons[i] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6},
               f8,
               f11,
               f9,
               f12,
               p_273591_,
               p_273313_,
               p_273589_,
               Direction.SOUTH
            );
         }
      }

      public void compile(PoseStack.Pose p_171333_, VertexConsumer p_171334_, int p_171335_, int p_171336_, int p_344599_) {
         Matrix4f matrix4f = p_171333_.pose();
         Vector3f vector3f = new Vector3f();

         for (ModelPart.Polygon modelpart$polygon : this.polygons) {
            Vector3f vector3f1 = p_171333_.transformNormal(modelpart$polygon.normal, vector3f);
            float f = vector3f1.x();
            float f1 = vector3f1.y();
            float f2 = vector3f1.z();

            for (ModelPart.Vertex modelpart$vertex : modelpart$polygon.vertices) {
               float f3 = modelpart$vertex.worldX();
               float f4 = modelpart$vertex.worldY();
               float f5 = modelpart$vertex.worldZ();
               Vector3f vector3f2 = matrix4f.transformPosition(f3, f4, f5, vector3f);
               p_171334_.addVertex(
                  vector3f2.x(), vector3f2.y(), vector3f2.z(), p_344599_, modelpart$vertex.u, modelpart$vertex.v, p_171336_, p_171335_, f, f1, f2
               );
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Polygon(ModelPart.Vertex[] vertices, Vector3fc normal) {
      public Polygon(
         ModelPart.Vertex[] p_104362_,
         float p_104363_,
         float p_104364_,
         float p_104365_,
         float p_104366_,
         float p_104367_,
         float p_104368_,
         boolean p_104369_,
         Direction p_104370_
      ) {
         this(p_104362_, (p_104369_ ? mirrorFacing(p_104370_) : p_104370_).getUnitVec3f());
         float f = 0.0F / p_104367_;
         float f1 = 0.0F / p_104368_;
         p_104362_[0] = p_104362_[0].remap(p_104365_ / p_104367_ - f, p_104364_ / p_104368_ + f1);
         p_104362_[1] = p_104362_[1].remap(p_104363_ / p_104367_ + f, p_104364_ / p_104368_ + f1);
         p_104362_[2] = p_104362_[2].remap(p_104363_ / p_104367_ + f, p_104366_ / p_104368_ - f1);
         p_104362_[3] = p_104362_[3].remap(p_104365_ / p_104367_ - f, p_104366_ / p_104368_ - f1);
         if (p_104369_) {
            int i = p_104362_.length;

            for (int j = 0; j < i / 2; j++) {
               ModelPart.Vertex modelpart$vertex = p_104362_[j];
               p_104362_[j] = p_104362_[i - 1 - j];
               p_104362_[i - 1 - j] = modelpart$vertex;
            }
         }
      }

      private static Direction mirrorFacing(Direction p_428375_) {
         return p_428375_.getAxis() == Direction.Axis.X ? p_428375_.getOpposite() : p_428375_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Vertex(float x, float y, float z, float u, float v) {
      public static final float SCALE_FACTOR = 16.0F;

      public ModelPart.Vertex remap(float p_104385_, float p_104386_) {
         return new ModelPart.Vertex(this.x, this.y, this.z, p_104385_, p_104386_);
      }

      public float worldX() {
         return this.x / 16.0F;
      }

      public float worldY() {
         return this.y / 16.0F;
      }

      public float worldZ() {
         return this.z / 16.0F;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Visitor {
      void visit(PoseStack.Pose var1, String var2, int var3, ModelPart.Cube var4);
   }
}
