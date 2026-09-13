package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.LevelReader;

public class CatLieOnBlockGoal extends MoveToBlockGoal {
   private final Cat cat;

   public CatLieOnBlockGoal(final Cat cat, final double speedModifier, final int searchRange) {
      super(cat, speedModifier, searchRange, 6);
      this.cat = cat;
      this.verticalSearchStart = -2;
      this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      return this.cat.isTame() && !this.cat.isOrderedToSit() && !this.cat.isLying() && super.canUse();
   }

   @Override
   public void start() {
      super.start();
      this.cat.setInSittingPose(false);
   }

   @Override
   protected int nextStartTick(final PathfinderMob mob) {
      return 40;
   }

   @Override
   public void stop() {
      super.stop();
      this.cat.setLying(false);
   }

   @Override
   public void tick() {
      super.tick();
      this.cat.setInSittingPose(false);
      if (!this.isReachedTarget()) {
         this.cat.setLying(false);
      } else if (!this.cat.isLying()) {
         this.cat.setLying(true);
      }
   }

   @Override
   protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
      return level.isEmptyBlock(pos.above()) && level.getBlockState(pos).is(BlockTags.CATS_CAN_LIE_ON);
   }
}
