package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AnimalPanic<E extends PathfinderMob> extends Behavior<E> {
   private static final int PANIC_MIN_DURATION = 100;
   private static final int PANIC_MAX_DURATION = 120;
   private static final int PANIC_DISTANCE_HORIZONTAL = 5;
   private static final int PANIC_DISTANCE_VERTICAL = 4;
   private final float speedMultiplier;
   private final Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes;
   private final Function<E, Vec3> positionGetter;

   public AnimalPanic(float p_147385_) {
      this(p_147385_, p_341293_ -> DamageTypeTags.PANIC_CAUSES, p_405308_ -> LandRandomPos.getPos(p_405308_, 5, 4));
   }

   public AnimalPanic(float p_406914_, int p_410458_) {
      this(
         p_406914_,
         p_405304_ -> DamageTypeTags.PANIC_CAUSES,
         p_405307_ -> AirAndWaterRandomPos.getPos(
            p_405307_, 5, 4, p_410458_, p_405307_.getViewVector(0.0F).x, p_405307_.getViewVector(0.0F).z, (float) (Math.PI / 2)
         )
      );
   }

   public AnimalPanic(float p_275357_, Function<PathfinderMob, TagKey<DamageType>> p_343027_) {
      this(p_275357_, p_343027_, p_405305_ -> LandRandomPos.getPos(p_405305_, 5, 4));
   }

   public AnimalPanic(float p_406867_, Function<PathfinderMob, TagKey<DamageType>> p_407651_, Function<E, Vec3> p_406875_) {
      super(Map.of(MemoryModuleType.IS_PANICKING, MemoryStatus.REGISTERED, MemoryModuleType.HURT_BY, MemoryStatus.REGISTERED), 100, 120);
      this.speedMultiplier = p_406867_;
      this.panicCausingDamageTypes = p_407651_;
      this.positionGetter = p_406875_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_275286_, E p_275721_) {
      return p_275721_.getBrain()
            .getMemory(MemoryModuleType.HURT_BY)
            .map(p_341295_ -> p_341295_.is(this.panicCausingDamageTypes.apply(p_275721_)))
            .orElse(false)
         || p_275721_.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected boolean canStillUse(ServerLevel p_147391_, E p_147392_, long p_147393_) {
      return true;
   }

   protected void start(ServerLevel p_147399_, E p_147400_, long p_147401_) {
      p_147400_.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
      p_147400_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      p_147400_.getNavigation().stop();
   }

   protected void stop(ServerLevel p_217118_, E p_217119_, long p_217120_) {
      Brain<?> brain = p_217119_.getBrain();
      brain.eraseMemory(MemoryModuleType.IS_PANICKING);
   }

   protected void tick(ServerLevel p_147403_, E p_147404_, long p_147405_) {
      if (p_147404_.getNavigation().isDone()) {
         Vec3 vec3 = this.getPanicPos(p_147404_, p_147403_);
         if (vec3 != null) {
            p_147404_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, this.speedMultiplier, 0));
         }
      }
   }

   private @Nullable Vec3 getPanicPos(E p_196639_, ServerLevel p_196640_) {
      if (p_196639_.isOnFire()) {
         Optional<Vec3> optional = this.lookForWater(p_196640_, p_196639_).map(Vec3::atBottomCenterOf);
         if (optional.isPresent()) {
            return optional.get();
         }
      }

      return this.positionGetter.apply(p_196639_);
   }

   private Optional<BlockPos> lookForWater(BlockGetter p_196642_, Entity p_196643_) {
      BlockPos blockpos = p_196643_.blockPosition();
      if (!p_196642_.getBlockState(blockpos).getCollisionShape(p_196642_, blockpos).isEmpty()) {
         return Optional.empty();
      }

      Predicate<BlockPos> predicate;
      if (Mth.ceil(p_196643_.getBbWidth()) == 2) {
         predicate = p_284705_ -> BlockPos.squareOutSouthEast(p_284705_).allMatch(p_196646_ -> p_196642_.getFluidState(p_196646_).is(FluidTags.WATER));
      } else {
         predicate = p_284707_ -> p_196642_.getFluidState(p_284707_).is(FluidTags.WATER);
      }

      return BlockPos.findClosestMatch(blockpos, 5, 1, predicate);
   }
}
