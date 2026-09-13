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

   public AnimalPanic(final float speedMultiplier) {
      this(speedMultiplier, mob -> DamageTypeTags.PANIC_CAUSES, mob -> LandRandomPos.getPos(mob, 5, 4));
   }

   public AnimalPanic(final float speedMultiplier, final int flyHeight) {
      this(
         speedMultiplier,
         mob -> DamageTypeTags.PANIC_CAUSES,
         mob -> AirAndWaterRandomPos.getPos(mob, 5, 4, flyHeight, mob.getViewVector(0.0F).x, mob.getViewVector(0.0F).z, (float) (Math.PI / 2))
      );
   }

   public AnimalPanic(final float speedMultiplier, final Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes) {
      this(speedMultiplier, panicCausingDamageTypes, mob -> LandRandomPos.getPos(mob, 5, 4));
   }

   public AnimalPanic(
      final float speedMultiplier, final Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes, final Function<E, Vec3> positionGetter
   ) {
      super(Map.of(MemoryModuleType.IS_PANICKING, MemoryStatus.REGISTERED, MemoryModuleType.HURT_BY, MemoryStatus.REGISTERED), 100, 120);
      this.speedMultiplier = speedMultiplier;
      this.panicCausingDamageTypes = panicCausingDamageTypes;
      this.positionGetter = positionGetter;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final E body) {
      return body.getBrain().getMemory(MemoryModuleType.HURT_BY).map(d -> d.is(this.panicCausingDamageTypes.apply(body))).orElse(false)
         || body.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected boolean canStillUse(final ServerLevel level, final E body, final long timestamp) {
      return true;
   }

   protected void start(final ServerLevel level, final E body, final long timestamp) {
      body.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      body.getNavigation().stop();
   }

   protected void stop(final ServerLevel level, final E body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      brain.eraseMemory(MemoryModuleType.IS_PANICKING);
   }

   protected void tick(final ServerLevel level, final E body, final long timestamp) {
      if (body.getNavigation().isDone()) {
         Vec3 panicToPos = this.getPanicPos(body, level);
         if (panicToPos != null) {
            body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(panicToPos, this.speedMultiplier, 0));
         }
      }
   }

   private @Nullable Vec3 getPanicPos(final E body, final ServerLevel level) {
      if (body.isOnFire()) {
         Optional<Vec3> nearestWater = this.lookForWater(level, body).map(Vec3::atBottomCenterOf);
         if (nearestWater.isPresent()) {
            return nearestWater.get();
         }
      }

      return this.positionGetter.apply(body);
   }

   private Optional<BlockPos> lookForWater(final BlockGetter level, final Entity mob) {
      BlockPos mobPosition = mob.blockPosition();
      if (!level.getBlockState(mobPosition).getCollisionShape(level, mobPosition).isEmpty()) {
         return Optional.empty();
      }

      Predicate<BlockPos> posPredicate;
      if (Mth.ceil(mob.getBbWidth()) == 2) {
         posPredicate = from -> BlockPos.squareOutSouthEast(from).allMatch(pos -> level.getFluidState(pos).is(FluidTags.WATER));
      } else {
         posPredicate = pos -> level.getFluidState(pos).is(FluidTags.WATER);
      }

      return BlockPos.findClosestMatch(mobPosition, 5, 1, posPredicate);
   }
}
