package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class BlockPosTracker implements PositionTracker {
   private final BlockPos blockPos;
   private final Vec3 centerPosition;

   public BlockPosTracker(BlockPos p_22676_) {
      this.blockPos = p_22676_.immutable();
      this.centerPosition = Vec3.atCenterOf(p_22676_);
   }

   public BlockPosTracker(Vec3 p_251060_) {
      this.blockPos = BlockPos.containing(p_251060_);
      this.centerPosition = p_251060_;
   }

   @Override
   public Vec3 currentPosition() {
      return this.centerPosition;
   }

   @Override
   public BlockPos currentBlockPosition() {
      return this.blockPos;
   }

   @Override
   public boolean isVisibleBy(LivingEntity p_22679_) {
      return true;
   }

   @Override
   public String toString() {
      return "BlockPosTracker{blockPos=" + this.blockPos + ", centerPosition=" + this.centerPosition + "}";
   }
}
