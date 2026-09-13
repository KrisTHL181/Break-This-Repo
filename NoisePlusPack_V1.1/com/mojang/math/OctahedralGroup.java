package com.mojang.math;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

public enum OctahedralGroup implements StringRepresentable {
   IDENTITY("identity", SymmetricGroup3.P123, false, false, false),
   ROT_180_FACE_XY("rot_180_face_xy", SymmetricGroup3.P123, true, true, false),
   ROT_180_FACE_XZ("rot_180_face_xz", SymmetricGroup3.P123, true, false, true),
   ROT_180_FACE_YZ("rot_180_face_yz", SymmetricGroup3.P123, false, true, true),
   ROT_120_NNN("rot_120_nnn", SymmetricGroup3.P231, false, false, false),
   ROT_120_NNP("rot_120_nnp", SymmetricGroup3.P312, true, false, true),
   ROT_120_NPN("rot_120_npn", SymmetricGroup3.P312, false, true, true),
   ROT_120_NPP("rot_120_npp", SymmetricGroup3.P231, true, false, true),
   ROT_120_PNN("rot_120_pnn", SymmetricGroup3.P312, true, true, false),
   ROT_120_PNP("rot_120_pnp", SymmetricGroup3.P231, true, true, false),
   ROT_120_PPN("rot_120_ppn", SymmetricGroup3.P231, false, true, true),
   ROT_120_PPP("rot_120_ppp", SymmetricGroup3.P312, false, false, false),
   ROT_180_EDGE_XY_NEG("rot_180_edge_xy_neg", SymmetricGroup3.P213, true, true, true),
   ROT_180_EDGE_XY_POS("rot_180_edge_xy_pos", SymmetricGroup3.P213, false, false, true),
   ROT_180_EDGE_XZ_NEG("rot_180_edge_xz_neg", SymmetricGroup3.P321, true, true, true),
   ROT_180_EDGE_XZ_POS("rot_180_edge_xz_pos", SymmetricGroup3.P321, false, true, false),
   ROT_180_EDGE_YZ_NEG("rot_180_edge_yz_neg", SymmetricGroup3.P132, true, true, true),
   ROT_180_EDGE_YZ_POS("rot_180_edge_yz_pos", SymmetricGroup3.P132, true, false, false),
   ROT_90_X_NEG("rot_90_x_neg", SymmetricGroup3.P132, false, false, true),
   ROT_90_X_POS("rot_90_x_pos", SymmetricGroup3.P132, false, true, false),
   ROT_90_Y_NEG("rot_90_y_neg", SymmetricGroup3.P321, true, false, false),
   ROT_90_Y_POS("rot_90_y_pos", SymmetricGroup3.P321, false, false, true),
   ROT_90_Z_NEG("rot_90_z_neg", SymmetricGroup3.P213, false, true, false),
   ROT_90_Z_POS("rot_90_z_pos", SymmetricGroup3.P213, true, false, false),
   INVERSION("inversion", SymmetricGroup3.P123, true, true, true),
   INVERT_X("invert_x", SymmetricGroup3.P123, true, false, false),
   INVERT_Y("invert_y", SymmetricGroup3.P123, false, true, false),
   INVERT_Z("invert_z", SymmetricGroup3.P123, false, false, true),
   ROT_60_REF_NNN("rot_60_ref_nnn", SymmetricGroup3.P312, true, true, true),
   ROT_60_REF_NNP("rot_60_ref_nnp", SymmetricGroup3.P231, true, false, false),
   ROT_60_REF_NPN("rot_60_ref_npn", SymmetricGroup3.P231, false, false, true),
   ROT_60_REF_NPP("rot_60_ref_npp", SymmetricGroup3.P312, false, false, true),
   ROT_60_REF_PNN("rot_60_ref_pnn", SymmetricGroup3.P231, false, true, false),
   ROT_60_REF_PNP("rot_60_ref_pnp", SymmetricGroup3.P312, true, false, false),
   ROT_60_REF_PPN("rot_60_ref_ppn", SymmetricGroup3.P312, false, true, false),
   ROT_60_REF_PPP("rot_60_ref_ppp", SymmetricGroup3.P231, true, true, true),
   SWAP_XY("swap_xy", SymmetricGroup3.P213, false, false, false),
   SWAP_YZ("swap_yz", SymmetricGroup3.P132, false, false, false),
   SWAP_XZ("swap_xz", SymmetricGroup3.P321, false, false, false),
   SWAP_NEG_XY("swap_neg_xy", SymmetricGroup3.P213, true, true, false),
   SWAP_NEG_YZ("swap_neg_yz", SymmetricGroup3.P132, false, true, true),
   SWAP_NEG_XZ("swap_neg_xz", SymmetricGroup3.P321, true, false, true),
   ROT_90_REF_X_NEG("rot_90_ref_x_neg", SymmetricGroup3.P132, true, false, true),
   ROT_90_REF_X_POS("rot_90_ref_x_pos", SymmetricGroup3.P132, true, true, false),
   ROT_90_REF_Y_NEG("rot_90_ref_y_neg", SymmetricGroup3.P321, true, true, false),
   ROT_90_REF_Y_POS("rot_90_ref_y_pos", SymmetricGroup3.P321, false, true, true),
   ROT_90_REF_Z_NEG("rot_90_ref_z_neg", SymmetricGroup3.P213, false, true, true),
   ROT_90_REF_Z_POS("rot_90_ref_z_pos", SymmetricGroup3.P213, true, false, true);

   public static final OctahedralGroup BLOCK_ROT_X_270 = ROT_90_X_POS;
   public static final OctahedralGroup BLOCK_ROT_X_180 = ROT_180_FACE_YZ;
   public static final OctahedralGroup BLOCK_ROT_X_90 = ROT_90_X_NEG;
   public static final OctahedralGroup BLOCK_ROT_Y_270 = ROT_90_Y_POS;
   public static final OctahedralGroup BLOCK_ROT_Y_180 = ROT_180_FACE_XZ;
   public static final OctahedralGroup BLOCK_ROT_Y_90 = ROT_90_Y_NEG;
   public static final OctahedralGroup BLOCK_ROT_Z_270 = ROT_90_Z_POS;
   public static final OctahedralGroup BLOCK_ROT_Z_180 = ROT_180_FACE_XY;
   public static final OctahedralGroup BLOCK_ROT_Z_90 = ROT_90_Z_NEG;
   private final Matrix3fc transformation;
   private final String name;
   private @Nullable Map<Direction, Direction> rotatedDirections;
   private final boolean invertX;
   private final boolean invertY;
   private final boolean invertZ;
   private final SymmetricGroup3 permutation;
   private static final OctahedralGroup[][] CAYLEY_TABLE = Util.make(
      () -> {
         OctahedralGroup[] aoctahedralgroup = values();
         OctahedralGroup[][] aoctahedralgroup1 = new OctahedralGroup[aoctahedralgroup.length][aoctahedralgroup.length];
         Map<Integer, OctahedralGroup> map = Arrays.stream(aoctahedralgroup)
            .collect(Collectors.toMap(OctahedralGroup::trace, p_174950_ -> (OctahedralGroup)p_174950_));

         for (OctahedralGroup octahedralgroup : aoctahedralgroup) {
            for (OctahedralGroup octahedralgroup1 : aoctahedralgroup) {
               SymmetricGroup3 symmetricgroup3 = octahedralgroup1.permutation.compose(octahedralgroup.permutation);
               boolean flag = octahedralgroup.inverts(Direction.Axis.X) ^ octahedralgroup1.inverts(octahedralgroup.permutation.permuteAxis(Direction.Axis.X));
               boolean flag1 = octahedralgroup.inverts(Direction.Axis.Y) ^ octahedralgroup1.inverts(octahedralgroup.permutation.permuteAxis(Direction.Axis.Y));
               boolean flag2 = octahedralgroup.inverts(Direction.Axis.Z) ^ octahedralgroup1.inverts(octahedralgroup.permutation.permuteAxis(Direction.Axis.Z));
               aoctahedralgroup1[octahedralgroup.ordinal()][octahedralgroup1.ordinal()] = map.get(trace(flag, flag1, flag2, symmetricgroup3));
            }
         }

         return aoctahedralgroup1;
      }
   );
   private static final OctahedralGroup[] INVERSE_TABLE = Arrays.stream(values())
      .map(p_56536_ -> Arrays.stream(values()).filter(p_174947_ -> p_56536_.compose(p_174947_) == IDENTITY).findAny().get())
      .toArray(OctahedralGroup[]::new);

   OctahedralGroup(final String p_56513_, final SymmetricGroup3 p_56514_, final boolean p_56515_, final boolean p_56516_, final boolean p_56517_) {
      this.name = p_56513_;
      this.invertX = p_56515_;
      this.invertY = p_56516_;
      this.invertZ = p_56517_;
      this.permutation = p_56514_;
      this.transformation = new Matrix3f().scaling(p_56515_ ? -1.0F : 1.0F, p_56516_ ? -1.0F : 1.0F, p_56517_ ? -1.0F : 1.0F).mul(p_56514_.transformation());
   }

   private static int trace(boolean p_450915_, boolean p_450544_, boolean p_454481_, SymmetricGroup3 p_454279_) {
      int i = (p_454481_ ? 4 : 0) + (p_450544_ ? 2 : 0) + (p_450915_ ? 1 : 0);
      return p_454279_.ordinal() << 3 | i;
   }

   private int trace() {
      return trace(this.invertX, this.invertY, this.invertZ, this.permutation);
   }

   public OctahedralGroup compose(OctahedralGroup p_56522_) {
      return CAYLEY_TABLE[this.ordinal()][p_56522_.ordinal()];
   }

   public OctahedralGroup inverse() {
      return INVERSE_TABLE[this.ordinal()];
   }

   public Matrix3fc transformation() {
      return this.transformation;
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public Direction rotate(Direction p_56529_) {
      if (this.rotatedDirections == null) {
         this.rotatedDirections = Util.makeEnumMap(Direction.class, p_447715_ -> {
            Direction.Axis direction$axis = p_447715_.getAxis();
            Direction.AxisDirection direction$axisdirection = p_447715_.getAxisDirection();
            Direction.Axis direction$axis1 = this.permutation.inverse().permuteAxis(direction$axis);
            Direction.AxisDirection direction$axisdirection1 = this.inverts(direction$axis1) ? direction$axisdirection.opposite() : direction$axisdirection;
            return Direction.fromAxisAndDirection(direction$axis1, direction$axisdirection1);
         });
      }

      return this.rotatedDirections.get(p_56529_);
   }

   public Vector3i rotate(Vector3i p_454126_) {
      this.permutation.permuteVector(p_454126_);
      p_454126_.x = p_454126_.x * (this.invertX ? -1 : 1);
      p_454126_.y = p_454126_.y * (this.invertY ? -1 : 1);
      p_454126_.z = p_454126_.z * (this.invertZ ? -1 : 1);
      return p_454126_;
   }

   public boolean inverts(Direction.Axis p_56527_) {
      return switch (p_56527_) {
         case X -> this.invertX;
         case Y -> this.invertY;
         case Z -> this.invertZ;
      };
   }

   public SymmetricGroup3 permutation() {
      return this.permutation;
   }

   public FrontAndTop rotate(FrontAndTop p_56531_) {
      return FrontAndTop.fromFrontAndTop(this.rotate(p_56531_.front()), this.rotate(p_56531_.top()));
   }
}
