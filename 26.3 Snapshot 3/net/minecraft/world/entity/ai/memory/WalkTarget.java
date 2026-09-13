package net.minecraft.world.entity.ai.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.phys.Vec3;

public class WalkTarget {
   private final PositionTracker target;
   private final float speedModifier;
   private final int closeEnoughDist;

   public WalkTarget(final BlockPos target, final float speedModifier, final int closeEnoughDist) {
      this(new BlockPosTracker(target), speedModifier, closeEnoughDist);
   }

   public WalkTarget(final Vec3 target, final float speedModifier, final int closeEnoughDist) {
      this(new BlockPosTracker(BlockPos.containing(target)), speedModifier, closeEnoughDist);
   }

   public WalkTarget(final Entity target, final float speedModifier, final int closeEnoughDist) {
      this(new EntityTracker(target, false), speedModifier, closeEnoughDist);
   }

   public WalkTarget(final PositionTracker target, final float speedModifier, final int closeEnoughDist) {
      this.target = target;
      this.speedModifier = speedModifier;
      this.closeEnoughDist = closeEnoughDist;
   }

   public PositionTracker getTarget() {
      return this.target;
   }

   public float getSpeedModifier() {
      return this.speedModifier;
   }

   public int getCloseEnoughDist() {
      return this.closeEnoughDist;
   }
}
