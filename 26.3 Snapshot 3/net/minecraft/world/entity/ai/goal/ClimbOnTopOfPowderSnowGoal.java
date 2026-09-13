package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

public class ClimbOnTopOfPowderSnowGoal extends Goal {
   private final Mob mob;
   private final Level level;

   public ClimbOnTopOfPowderSnowGoal(final Mob mob, final Level level) {
      this.mob = mob;
      this.level = level;
      this.setFlags(EnumSet.of(Goal.Flag.JUMP));
   }

   @Override
   public boolean canUse() {
      boolean inPowderSnow = this.mob.wasInPowderSnow || this.mob.isInPowderSnow;
      if (inPowderSnow && this.mob.is(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
         BlockPos above = this.mob.blockPosition().above();
         BlockState aboveBlockState = this.level.getBlockState(above);
         return aboveBlockState.is(Blocks.POWDER_SNOW) || aboveBlockState.getCollisionShape(this.level, above) == Shapes.empty();
      } else {
         return false;
      }
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      this.mob.getJumpControl().jump();
   }
}
