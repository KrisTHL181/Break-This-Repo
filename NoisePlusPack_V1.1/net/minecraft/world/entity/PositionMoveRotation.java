package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public record PositionMoveRotation(Vec3 position, Vec3 deltaMovement, float yRot, float xRot) {
   public static final StreamCodec<FriendlyByteBuf, PositionMoveRotation> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC,
      PositionMoveRotation::position,
      Vec3.STREAM_CODEC,
      PositionMoveRotation::deltaMovement,
      ByteBufCodecs.FLOAT,
      PositionMoveRotation::yRot,
      ByteBufCodecs.FLOAT,
      PositionMoveRotation::xRot,
      PositionMoveRotation::new
   );

   public static PositionMoveRotation of(Entity p_361379_) {
      return p_361379_.isInterpolating()
         ? new PositionMoveRotation(
            p_361379_.getInterpolation().position(), p_361379_.getKnownMovement(), p_361379_.getInterpolation().yRot(), p_361379_.getInterpolation().xRot()
         )
         : new PositionMoveRotation(p_361379_.position(), p_361379_.getKnownMovement(), p_361379_.getYRot(), p_361379_.getXRot());
   }

   public PositionMoveRotation withRotation(float p_427362_, float p_424871_) {
      return new PositionMoveRotation(this.position(), this.deltaMovement(), p_427362_, p_424871_);
   }

   public static PositionMoveRotation of(TeleportTransition p_368179_) {
      return new PositionMoveRotation(p_368179_.position(), p_368179_.deltaMovement(), p_368179_.yRot(), p_368179_.xRot());
   }

   public static PositionMoveRotation calculateAbsolute(PositionMoveRotation p_364389_, PositionMoveRotation p_363716_, Set<Relative> p_370218_) {
      double d0 = p_370218_.contains(Relative.X) ? p_364389_.position.x : 0.0;
      double d1 = p_370218_.contains(Relative.Y) ? p_364389_.position.y : 0.0;
      double d2 = p_370218_.contains(Relative.Z) ? p_364389_.position.z : 0.0;
      float f = p_370218_.contains(Relative.Y_ROT) ? p_364389_.yRot : 0.0F;
      float f1 = p_370218_.contains(Relative.X_ROT) ? p_364389_.xRot : 0.0F;
      Vec3 vec3 = new Vec3(d0 + p_363716_.position.x, d1 + p_363716_.position.y, d2 + p_363716_.position.z);
      float f2 = f + p_363716_.yRot;
      float f3 = Mth.clamp(f1 + p_363716_.xRot, -90.0F, 90.0F);
      Vec3 vec31 = p_364389_.deltaMovement;
      if (p_370218_.contains(Relative.ROTATE_DELTA)) {
         float f4 = p_364389_.yRot - f2;
         float f5 = p_364389_.xRot - f3;
         vec31 = vec31.xRot((float)Math.toRadians(f5));
         vec31 = vec31.yRot((float)Math.toRadians(f4));
      }

      Vec3 vec32 = new Vec3(
         calculateDelta(vec31.x, p_363716_.deltaMovement.x, p_370218_, Relative.DELTA_X),
         calculateDelta(vec31.y, p_363716_.deltaMovement.y, p_370218_, Relative.DELTA_Y),
         calculateDelta(vec31.z, p_363716_.deltaMovement.z, p_370218_, Relative.DELTA_Z)
      );
      return new PositionMoveRotation(vec3, vec32, f2, f3);
   }

   private static double calculateDelta(double p_366007_, double p_365256_, Set<Relative> p_365151_, Relative p_367876_) {
      return p_365151_.contains(p_367876_) ? p_366007_ + p_365256_ : p_365256_;
   }
}
