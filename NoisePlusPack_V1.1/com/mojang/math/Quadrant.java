package com.mojang.math;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public enum Quadrant {
   R0(0, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY),
   R90(1, OctahedralGroup.BLOCK_ROT_X_90, OctahedralGroup.BLOCK_ROT_Y_90, OctahedralGroup.BLOCK_ROT_Z_90),
   R180(2, OctahedralGroup.BLOCK_ROT_X_180, OctahedralGroup.BLOCK_ROT_Y_180, OctahedralGroup.BLOCK_ROT_Z_180),
   R270(3, OctahedralGroup.BLOCK_ROT_X_270, OctahedralGroup.BLOCK_ROT_Y_270, OctahedralGroup.BLOCK_ROT_Z_270);

   public static final Codec<Quadrant> CODEC = Codec.INT.comapFlatMap(p_394821_ -> {
      return switch (Mth.positiveModulo(p_394821_, 360)) {
         case 0 -> DataResult.success(R0);
         case 90 -> DataResult.success(R90);
         case 180 -> DataResult.success(R180);
         case 270 -> DataResult.success(R270);
         default -> DataResult.error(() -> "Invalid rotation " + p_394821_ + " found, only 0/90/180/270 allowed");
      };
   }, p_396271_ -> {
      return switch (p_396271_) {
         case R0 -> 0;
         case R90 -> 90;
         case R180 -> 180;
         case R270 -> 270;
      };
   });
   public final int shift;
   public final OctahedralGroup rotationX;
   public final OctahedralGroup rotationY;
   public final OctahedralGroup rotationZ;

   Quadrant(final int p_394613_, final OctahedralGroup p_455541_, final OctahedralGroup p_453227_, final OctahedralGroup p_458287_) {
      this.shift = p_394613_;
      this.rotationX = p_455541_;
      this.rotationY = p_453227_;
      this.rotationZ = p_458287_;
   }

   @Deprecated
   public static Quadrant parseJson(int p_393135_) {
      return switch (Mth.positiveModulo(p_393135_, 360)) {
         case 0 -> R0;
         case 90 -> R90;
         case 180 -> R180;
         case 270 -> R270;
         default -> throw new JsonParseException("Invalid rotation " + p_393135_ + " found, only 0/90/180/270 allowed");
      };
   }

   public static OctahedralGroup fromXYAngles(Quadrant p_459454_, Quadrant p_453490_) {
      return p_453490_.rotationY.compose(p_459454_.rotationX);
   }

   public static OctahedralGroup fromXYZAngles(Quadrant p_450214_, Quadrant p_450636_, Quadrant p_454752_) {
      return p_454752_.rotationZ.compose(p_450636_.rotationY.compose(p_450214_.rotationX));
   }

   public int rotateVertexIndex(int p_391588_) {
      return (p_391588_ + this.shift) % 4;
   }
}
