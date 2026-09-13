package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.Objects;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FaceBakery {
   private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5F, 0.5F, 0.5F);

   @VisibleForTesting
   static BlockElementFace.UVs defaultFaceUV(Vector3fc p_393926_, Vector3fc p_395023_, Direction p_393336_) {
      return switch (p_393336_) {
         case DOWN -> new BlockElementFace.UVs(p_393926_.x(), 16.0F - p_395023_.z(), p_395023_.x(), 16.0F - p_393926_.z());
         case UP -> new BlockElementFace.UVs(p_393926_.x(), p_393926_.z(), p_395023_.x(), p_395023_.z());
         case NORTH -> new BlockElementFace.UVs(16.0F - p_395023_.x(), 16.0F - p_395023_.y(), 16.0F - p_393926_.x(), 16.0F - p_393926_.y());
         case SOUTH -> new BlockElementFace.UVs(p_393926_.x(), 16.0F - p_395023_.y(), p_395023_.x(), 16.0F - p_393926_.y());
         case WEST -> new BlockElementFace.UVs(p_393926_.z(), 16.0F - p_395023_.y(), p_395023_.z(), 16.0F - p_393926_.y());
         case EAST -> new BlockElementFace.UVs(16.0F - p_395023_.z(), 16.0F - p_395023_.y(), 16.0F - p_393926_.z(), 16.0F - p_393926_.y());
      };
   }

   public static BakedQuad bakeQuad(
      ModelBaker.PartCache p_454391_,
      Vector3fc p_393181_,
      Vector3fc p_395026_,
      BlockElementFace p_111603_,
      TextureAtlasSprite p_111604_,
      Direction p_111605_,
      ModelState p_111606_,
      @Nullable BlockElementRotation p_111607_,
      boolean p_111608_,
      int p_364904_
   ) {
      BlockElementFace.UVs blockelementface$uvs = p_111603_.uvs();
      if (blockelementface$uvs == null) {
         blockelementface$uvs = defaultFaceUV(p_393181_, p_395026_, p_111605_);
      }

      Matrix4fc matrix4fc = p_111606_.inverseFaceTransformation(p_111605_);
      Vector3fc[] avector3fc = new Vector3fc[4];
      long[] along = new long[4];
      FaceInfo faceinfo = FaceInfo.fromFacing(p_111605_);

      for (int i = 0; i < 4; i++) {
         bakeVertex(
            i,
            faceinfo,
            blockelementface$uvs,
            p_111603_.rotation(),
            matrix4fc,
            p_393181_,
            p_395026_,
            p_111604_,
            p_111606_.transformation(),
            p_111607_,
            avector3fc,
            along,
            p_454391_
         );
      }

      Direction direction = calculateFacing(avector3fc);
      if (p_111607_ == null && direction != null) {
         recalculateWinding(avector3fc, along, direction);
      }

      return new BakedQuad(
         avector3fc[0],
         avector3fc[1],
         avector3fc[2],
         avector3fc[3],
         along[0],
         along[1],
         along[2],
         along[3],
         p_111603_.tintIndex(),
         Objects.requireNonNullElse(direction, Direction.UP),
         p_111604_,
         p_111608_,
         p_364904_
      );
   }

   private static void bakeVertex(
      int p_111622_,
      FaceInfo p_394082_,
      BlockElementFace.UVs p_395065_,
      Quadrant p_392208_,
      Matrix4fc p_396750_,
      Vector3fc p_457024_,
      Vector3fc p_461049_,
      TextureAtlasSprite p_111626_,
      Transformation p_111627_,
      @Nullable BlockElementRotation p_111628_,
      Vector3fc[] p_458310_,
      long[] p_458168_,
      ModelBaker.PartCache p_454380_
   ) {
      FaceInfo.VertexInfo faceinfo$vertexinfo = p_394082_.getVertexInfo(p_111622_);
      Vector3f vector3f = faceinfo$vertexinfo.select(p_457024_, p_461049_).div(16.0F);
      if (p_111628_ != null) {
         rotateVertexBy(vector3f, p_111628_.origin(), p_111628_.transform());
      }

      if (p_111627_ != Transformation.identity()) {
         rotateVertexBy(vector3f, BLOCK_MIDDLE, p_111627_.getMatrix());
      }

      float f = BlockElementFace.getU(p_395065_, p_392208_, p_111622_);
      float f1 = BlockElementFace.getV(p_395065_, p_392208_, p_111622_);
      float f2;
      float f3;
      if (MatrixUtil.isIdentity(p_396750_)) {
         f3 = f;
         f2 = f1;
      } else {
         Vector3f vector3f1 = p_396750_.transformPosition(new Vector3f(cornerToCenter(f), cornerToCenter(f1), 0.0F));
         f3 = centerToCorner(vector3f1.x);
         f2 = centerToCorner(vector3f1.y);
      }

      p_458310_[p_111622_] = p_454380_.vector(vector3f);
      p_458168_[p_111622_] = UVPair.pack(p_111626_.getU(f3), p_111626_.getV(f2));
   }

   private static float cornerToCenter(float p_393791_) {
      return p_393791_ - 0.5F;
   }

   private static float centerToCorner(float p_392923_) {
      return p_392923_ + 0.5F;
   }

   private static void rotateVertexBy(Vector3f p_393378_, Vector3fc p_396712_, Matrix4fc p_394901_) {
      p_393378_.sub(p_396712_);
      p_394901_.transformPosition(p_393378_);
      p_393378_.add(p_396712_);
   }

   private static @Nullable Direction calculateFacing(Vector3fc[] p_454938_) {
      Vector3f vector3f = new Vector3f();
      GeometryUtils.normal(p_454938_[0], p_454938_[1], p_454938_[2], vector3f);
      return findClosestDirection(vector3f);
   }

   private static @Nullable Direction findClosestDirection(Vector3f p_459147_) {
      if (!p_459147_.isFinite()) {
         return null;
      }

      Direction direction = null;
      float f = 0.0F;

      for (Direction direction1 : Direction.values()) {
         float f1 = p_459147_.dot(direction1.getUnitVec3f());
         if (f1 >= 0.0F && f1 > f) {
            f = f1;
            direction = direction1;
         }
      }

      return direction;
   }

   private static void recalculateWinding(Vector3fc[] p_458554_, long[] p_452400_, Direction p_111632_) {
      float f = 999.0F;
      float f1 = 999.0F;
      float f2 = 999.0F;
      float f3 = -999.0F;
      float f4 = -999.0F;
      float f5 = -999.0F;

      for (int i = 0; i < 4; i++) {
         Vector3fc vector3fc = p_458554_[i];
         float f6 = vector3fc.x();
         float f7 = vector3fc.y();
         float f8 = vector3fc.z();
         if (f6 < f) {
            f = f6;
         }

         if (f7 < f1) {
            f1 = f7;
         }

         if (f8 < f2) {
            f2 = f8;
         }

         if (f6 > f3) {
            f3 = f6;
         }

         if (f7 > f4) {
            f4 = f7;
         }

         if (f8 > f5) {
            f5 = f8;
         }
      }

      FaceInfo faceinfo = FaceInfo.fromFacing(p_111632_);

      for (int k = 0; k < 4; k++) {
         FaceInfo.VertexInfo faceinfo$vertexinfo = faceinfo.getVertexInfo(k);
         float f10 = faceinfo$vertexinfo.xFace().select(f, f1, f2, f3, f4, f5);
         float f11 = faceinfo$vertexinfo.yFace().select(f, f1, f2, f3, f4, f5);
         float f9 = faceinfo$vertexinfo.zFace().select(f, f1, f2, f3, f4, f5);
         int j = findVertex(p_458554_, k, f10, f11, f9);
         if (j == -1) {
            throw new IllegalStateException("Can't find vertex to swap");
         }

         if (j != k) {
            swap(p_458554_, j, k);
            swap(p_452400_, j, k);
         }
      }
   }

   private static int findVertex(Vector3fc[] p_451067_, int p_453462_, float p_453093_, float p_459169_, float p_450288_) {
      for (int i = p_453462_; i < 4; i++) {
         Vector3fc vector3fc = p_451067_[i];
         if (p_453093_ == vector3fc.x() && p_459169_ == vector3fc.y() && p_450288_ == vector3fc.z()) {
            return i;
         }
      }

      return -1;
   }

   private static void swap(Vector3fc[] p_451695_, int p_454299_, int p_450909_) {
      Vector3fc vector3fc = p_451695_[p_454299_];
      p_451695_[p_454299_] = p_451695_[p_450909_];
      p_451695_[p_450909_] = vector3fc;
   }

   private static void swap(long[] p_458318_, int p_460145_, int p_459121_) {
      long i = p_458318_[p_460145_];
      p_458318_[p_460145_] = p_458318_[p_459121_];
      p_458318_[p_459121_] = i;
   }
}
