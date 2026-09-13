package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PanicGoal extends Goal {
   public static final int WATER_CHECK_DISTANCE_VERTICAL = 1;
   protected final PathfinderMob mob;
   protected final double speedModifier;
   protected double posX;
   protected double posY;
   protected double posZ;
   protected boolean isRunning;
   private final Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes;

   public PanicGoal(PathfinderMob p_25691_, double p_25692_) {
      this(p_25691_, p_25692_, DamageTypeTags.PANIC_CAUSES);
   }

   public PanicGoal(PathfinderMob p_344360_, double p_345088_, TagKey<DamageType> p_345524_) {
      this(p_344360_, p_345088_, p_341380_ -> p_345524_);
   }

   public PanicGoal(PathfinderMob p_343339_, double p_343298_, Function<PathfinderMob, TagKey<DamageType>> p_344730_) {
      this.mob = p_343339_;
      this.speedModifier = p_343298_;
      this.panicCausingDamageTypes = p_344730_;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (!this.shouldPanic()) {
         return false;
      }

      if (this.mob.isOnFire()) {
         BlockPos blockpos = this.lookForWater(this.mob.level(), this.mob, 5);
         if (blockpos != null) {
            this.posX = blockpos.getX();
            this.posY = blockpos.getY();
            this.posZ = blockpos.getZ();
            return true;
         }
      }

      return this.findRandomPosition();
   }

   protected boolean shouldPanic() {
      return this.mob.getLastDamageSource() != null && this.mob.getLastDamageSource().is(this.panicCausingDamageTypes.apply(this.mob));
   }

   protected boolean findRandomPosition() {
      Vec3 vec3 = DefaultRandomPos.getPos(this.mob, 5, 4);
      if (vec3 == null) {
         return false;
      }

      this.posX = vec3.x;
      this.posY = vec3.y;
      this.posZ = vec3.z;
      return true;
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
      this.isRunning = true;
   }

   @Override
   public void stop() {
      this.isRunning = false;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.mob.getNavigation().isDone();
   }

   protected @Nullable BlockPos lookForWater(BlockGetter p_198173_, Entity p_198174_, int p_198175_) {
      BlockPos blockpos = p_198174_.blockPosition();
      return !p_198173_.getBlockState(blockpos).getCollisionShape(p_198173_, blockpos).isEmpty()
         ? null
         : BlockPos.findClosestMatch(p_198174_.blockPosition(), p_198175_, 1, p_196649_ -> p_198173_.getFluidState(p_196649_).is(FluidTags.WATER)).orElse(null);
   }
}
