package net.minecraft.client.resources.model;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Transformation;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public class BlockModelRotation implements ModelState {
   private static final Map<OctahedralGroup, BlockModelRotation> BY_GROUP_ORDINAL = Util.makeEnumMap(OctahedralGroup.class, BlockModelRotation::new);
   public static final BlockModelRotation IDENTITY = get(OctahedralGroup.IDENTITY);
   final OctahedralGroup orientation;
   final Transformation transformation;
   final Map<Direction, Matrix4fc> faceMapping = new EnumMap<>(Direction.class);
   final Map<Direction, Matrix4fc> inverseFaceMapping = new EnumMap<>(Direction.class);
   private final BlockModelRotation.WithUvLock withUvLock = new BlockModelRotation.WithUvLock(this);

   private BlockModelRotation(OctahedralGroup p_452905_) {
      this.orientation = p_452905_;
      if (p_452905_ != OctahedralGroup.IDENTITY) {
         this.transformation = new Transformation(new Matrix4f(p_452905_.transformation()));
      } else {
         this.transformation = Transformation.identity();
      }

      for (Direction direction : Direction.values()) {
         Matrix4fc matrix4fc = BlockMath.getFaceTransformation(this.transformation, direction).getMatrix();
         this.faceMapping.put(direction, matrix4fc);
         this.inverseFaceMapping.put(direction, matrix4fc.invertAffine(new Matrix4f()));
      }
   }

   @Override
   public Transformation transformation() {
      return this.transformation;
   }

   public static BlockModelRotation get(OctahedralGroup p_458471_) {
      return BY_GROUP_ORDINAL.get(p_458471_);
   }

   public ModelState withUvLock() {
      return this.withUvLock;
   }

   @Override
   public String toString() {
      return "simple[" + this.orientation.getSerializedName() + "]";
   }

   @OnlyIn(Dist.CLIENT)
   record WithUvLock(BlockModelRotation parent) implements ModelState {
      @Override
      public Transformation transformation() {
         return this.parent.transformation;
      }

      @Override
      public Matrix4fc faceTransformation(Direction p_392706_) {
         return this.parent.faceMapping.getOrDefault(p_392706_, NO_TRANSFORM);
      }

      @Override
      public Matrix4fc inverseFaceTransformation(Direction p_391398_) {
         return this.parent.inverseFaceMapping.getOrDefault(p_391398_, NO_TRANSFORM);
      }

      @Override
      public String toString() {
         return "uvLocked[" + this.parent.orientation.getSerializedName() + "]";
      }
   }
}
