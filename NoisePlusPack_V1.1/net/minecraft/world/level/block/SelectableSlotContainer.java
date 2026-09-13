package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface SelectableSlotContainer {
   int getRows();

   int getColumns();

   default OptionalInt getHitSlot(BlockHitResult p_427349_, Direction p_422483_) {
      return getRelativeHitCoordinatesForBlockFace(p_427349_, p_422483_).map(p_427292_ -> {
         int i = getSection(1.0F - p_427292_.y, this.getRows());
         int j = getSection(p_427292_.x, this.getColumns());
         return OptionalInt.of(j + i * this.getColumns());
      }).orElseGet(OptionalInt::empty);
   }

   private static Optional<Vec2> getRelativeHitCoordinatesForBlockFace(BlockHitResult p_429515_, Direction p_424579_) {
      Direction direction = p_429515_.getDirection();
      if (p_424579_ != direction) {
         return Optional.empty();
      }

      BlockPos blockpos = p_429515_.getBlockPos().relative(direction);
      Vec3 vec3 = p_429515_.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
      double d0 = vec3.x();
      double d1 = vec3.y();
      double d2 = vec3.z();

      return switch (direction) {
         case NORTH -> Optional.of(new Vec2((float)(1.0 - d0), (float)d1));
         case SOUTH -> Optional.of(new Vec2((float)d0, (float)d1));
         case WEST -> Optional.of(new Vec2((float)d2, (float)d1));
         case EAST -> Optional.of(new Vec2((float)(1.0 - d2), (float)d1));
         case DOWN, UP -> Optional.empty();
      };
   }

   private static int getSection(float p_429140_, int p_431400_) {
      float f = p_429140_ * 16.0F;
      float f1 = 16.0F / p_431400_;
      return Mth.clamp(Mth.floor(f / f1), 0, p_431400_ - 1);
   }
}
