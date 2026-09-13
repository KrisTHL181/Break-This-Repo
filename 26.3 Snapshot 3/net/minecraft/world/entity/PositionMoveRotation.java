package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.core.PositionAndRotation;
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

   public static PositionMoveRotation of(final Entity entity) {
      if (entity.isInterpolating()) {
         PositionAndRotation current = entity.getInterpolation().getCurrentPositionAndRotation();
         return new PositionMoveRotation(current.position(), entity.getKnownMovement(), current.yRot(), current.xRot());
      } else {
         return new PositionMoveRotation(entity.position(), entity.getKnownMovement(), entity.getYRot(), entity.getXRot());
      }
   }

   public PositionMoveRotation withRotation(final float yRot, final float xRot) {
      return new PositionMoveRotation(this.position(), this.deltaMovement(), yRot, xRot);
   }

   public static PositionMoveRotation of(final TeleportTransition transition) {
      return new PositionMoveRotation(transition.position(), transition.deltaMovement(), transition.yRot(), transition.xRot());
   }

   public static PositionMoveRotation calculateAbsolute(final PositionMoveRotation source, final PositionMoveRotation change, final Set<Relative> relatives) {
      double offsetX = relatives.contains(Relative.X) ? source.position.x : 0.0;
      double offsetY = relatives.contains(Relative.Y) ? source.position.y : 0.0;
      double offsetZ = relatives.contains(Relative.Z) ? source.position.z : 0.0;
      float offsetYRot = relatives.contains(Relative.Y_ROT) ? source.yRot : 0.0F;
      float offsetXRot = relatives.contains(Relative.X_ROT) ? source.xRot : 0.0F;
      Vec3 absolutePosition = new Vec3(offsetX + change.position.x, offsetY + change.position.y, offsetZ + change.position.z);
      float absoluteYRot = offsetYRot + change.yRot;
      float absoluteXRot = Mth.clamp(offsetXRot + change.xRot, -90.0F, 90.0F);
      Vec3 rotatedCurrentMovement = source.deltaMovement;
      if (relatives.contains(Relative.ROTATE_DELTA)) {
         float diffYRot = source.yRot - absoluteYRot;
         float diffXRot = source.xRot - absoluteXRot;
         rotatedCurrentMovement = rotatedCurrentMovement.xRot((float)Math.toRadians(diffXRot));
         rotatedCurrentMovement = rotatedCurrentMovement.yRot((float)Math.toRadians(diffYRot));
      }

      Vec3 absoluteDeltaMovement = new Vec3(
         calculateDelta(rotatedCurrentMovement.x, change.deltaMovement.x, relatives, Relative.DELTA_X),
         calculateDelta(rotatedCurrentMovement.y, change.deltaMovement.y, relatives, Relative.DELTA_Y),
         calculateDelta(rotatedCurrentMovement.z, change.deltaMovement.z, relatives, Relative.DELTA_Z)
      );
      return new PositionMoveRotation(absolutePosition, absoluteDeltaMovement, absoluteYRot, absoluteXRot);
   }

   private static double calculateDelta(final double currentDelta, final double deltaChange, final Set<Relative> relatives, final Relative relative) {
      return relatives.contains(relative) ? currentDelta + deltaChange : deltaChange;
   }
}
