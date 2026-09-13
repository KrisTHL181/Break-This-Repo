package com.mojang.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class Transformation {
   private final Matrix4fc matrix;
   public static final Codec<Transformation> CODEC = RecordCodecBuilder.create(
      p_269604_ -> p_269604_.group(
            ExtraCodecs.VECTOR3F.fieldOf("translation").forGetter(p_447724_ -> p_447724_.translation),
            ExtraCodecs.QUATERNIONF.fieldOf("left_rotation").forGetter(p_447726_ -> p_447726_.leftRotation),
            ExtraCodecs.VECTOR3F.fieldOf("scale").forGetter(p_447727_ -> p_447727_.scale),
            ExtraCodecs.QUATERNIONF.fieldOf("right_rotation").forGetter(p_447725_ -> p_447725_.rightRotation)
         )
         .apply(p_269604_, Transformation::new)
   );
   public static final Codec<Transformation> EXTENDED_CODEC = Codec.withAlternative(
      CODEC, ExtraCodecs.MATRIX4F.xmap(Transformation::new, Transformation::getMatrix)
   );
   private boolean decomposed;
   private @Nullable Vector3fc translation;
   private @Nullable Quaternionfc leftRotation;
   private @Nullable Vector3fc scale;
   private @Nullable Quaternionfc rightRotation;
   private static final Transformation IDENTITY = Util.make(() -> {
      Transformation transformation = new Transformation(new Matrix4f());
      transformation.translation = new Vector3f();
      transformation.leftRotation = new Quaternionf();
      transformation.scale = new Vector3f(1.0F, 1.0F, 1.0F);
      transformation.rightRotation = new Quaternionf();
      transformation.decomposed = true;
      return transformation;
   });

   public Transformation(@Nullable Matrix4fc p_393211_) {
      if (p_393211_ == null) {
         this.matrix = new Matrix4f();
      } else {
         this.matrix = p_393211_;
      }
   }

   public Transformation(@Nullable Vector3fc p_455816_, @Nullable Quaternionfc p_452471_, @Nullable Vector3fc p_458390_, @Nullable Quaternionfc p_454607_) {
      this.matrix = compose(p_455816_, p_452471_, p_458390_, p_454607_);
      this.translation = (Vector3fc)(p_455816_ != null ? p_455816_ : new Vector3f());
      this.leftRotation = (Quaternionfc)(p_452471_ != null ? p_452471_ : new Quaternionf());
      this.scale = (Vector3fc)(p_458390_ != null ? p_458390_ : new Vector3f(1.0F, 1.0F, 1.0F));
      this.rightRotation = (Quaternionfc)(p_454607_ != null ? p_454607_ : new Quaternionf());
      this.decomposed = true;
   }

   public static Transformation identity() {
      return IDENTITY;
   }

   public Transformation compose(Transformation p_121097_) {
      Matrix4f matrix4f = this.getMatrixCopy();
      matrix4f.mul(p_121097_.getMatrix());
      return new Transformation(matrix4f);
   }

   public @Nullable Transformation inverse() {
      if (this == IDENTITY) {
         return this;
      }

      Matrix4f matrix4f = this.getMatrixCopy().invertAffine();
      return matrix4f.isFinite() ? new Transformation(matrix4f) : null;
   }

   private void ensureDecomposed() {
      if (!this.decomposed) {
         float f = 1.0F / this.matrix.m33();
         Triple<Quaternionf, Vector3f, Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(this.matrix).scale(f));
         this.translation = this.matrix.getTranslation(new Vector3f()).mul(f);
         this.leftRotation = new Quaternionf((Quaternionfc)triple.getLeft());
         this.scale = new Vector3f((Vector3fc)triple.getMiddle());
         this.rightRotation = new Quaternionf((Quaternionfc)triple.getRight());
         this.decomposed = true;
      }
   }

   private static Matrix4f compose(
      @Nullable Vector3fc p_456190_, @Nullable Quaternionfc p_457962_, @Nullable Vector3fc p_451996_, @Nullable Quaternionfc p_453189_
   ) {
      Matrix4f matrix4f = new Matrix4f();
      if (p_456190_ != null) {
         matrix4f.translation(p_456190_);
      }

      if (p_457962_ != null) {
         matrix4f.rotate(p_457962_);
      }

      if (p_451996_ != null) {
         matrix4f.scale(p_451996_);
      }

      if (p_453189_ != null) {
         matrix4f.rotate(p_453189_);
      }

      return matrix4f;
   }

   public Matrix4fc getMatrix() {
      return this.matrix;
   }

   public Matrix4f getMatrixCopy() {
      return new Matrix4f(this.matrix);
   }

   public Vector3fc getTranslation() {
      this.ensureDecomposed();
      return this.translation;
   }

   public Quaternionfc getLeftRotation() {
      this.ensureDecomposed();
      return this.leftRotation;
   }

   public Vector3fc getScale() {
      this.ensureDecomposed();
      return this.scale;
   }

   public Quaternionfc getRightRotation() {
      this.ensureDecomposed();
      return this.rightRotation;
   }

   @Override
   public boolean equals(Object p_121108_) {
      if (this == p_121108_) {
         return true;
      } else if (p_121108_ != null && this.getClass() == p_121108_.getClass()) {
         Transformation transformation = (Transformation)p_121108_;
         return Objects.equals(this.matrix, transformation.matrix);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.matrix);
   }

   public Transformation slerp(Transformation p_175938_, float p_175939_) {
      return new Transformation(
         this.getTranslation().lerp(p_175938_.getTranslation(), p_175939_, new Vector3f()),
         this.getLeftRotation().slerp(p_175938_.getLeftRotation(), p_175939_, new Quaternionf()),
         this.getScale().lerp(p_175938_.getScale(), p_175939_, new Vector3f()),
         this.getRightRotation().slerp(p_175938_.getRightRotation(), p_175939_, new Quaternionf())
      );
   }
}
