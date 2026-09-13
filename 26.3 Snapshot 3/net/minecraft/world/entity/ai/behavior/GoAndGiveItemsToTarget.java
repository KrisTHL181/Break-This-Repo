package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

public class GoAndGiveItemsToTarget<E extends LivingEntity> extends Behavior<E> {
   private static final int CLOSE_ENOUGH_DISTANCE_TO_TARGET = 3;
   private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
   private final float speedModifier;
   private final GoAndGiveItemsToTarget.ItemThrower<E> itemThrower;
   private final MemoryModuleType<Integer> cooldownMemory;
   private final int cooldownDuration;
   private final Predicate<E> hasItemPredicate;

   public GoAndGiveItemsToTarget(
      final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter,
      final float speedModifier,
      final int timeoutDuration,
      final GoAndGiveItemsToTarget.ItemThrower<E> itemThrower,
      final MemoryModuleType<Integer> cooldownMemory,
      final int cooldownDuration,
      final Predicate<E> hasItemPredicate
   ) {
      super(
         Map.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            cooldownMemory,
            MemoryStatus.REGISTERED
         ),
         timeoutDuration
      );
      this.targetPositionGetter = targetPositionGetter;
      this.speedModifier = speedModifier;
      this.itemThrower = itemThrower;
      this.cooldownMemory = cooldownMemory;
      this.cooldownDuration = cooldownDuration;
      this.hasItemPredicate = hasItemPredicate;
   }

   @Override
   protected boolean checkExtraStartConditions(final ServerLevel level, final E body) {
      return this.canThrowItemToTarget(body);
   }

   @Override
   protected boolean canStillUse(final ServerLevel level, final E body, final long timestamp) {
      return this.canThrowItemToTarget(body);
   }

   @Override
   protected void start(final ServerLevel level, final E body, final long timestamp) {
      this.targetPositionGetter
         .apply(body)
         .ifPresent(positionTracker -> BehaviorUtils.setWalkAndLookTargetMemories(body, positionTracker, this.speedModifier, 3));
   }

   @Override
   protected void tick(final ServerLevel level, final E body, final long timestamp) {
      Optional<PositionTracker> targetPosition = this.targetPositionGetter.apply(body);
      if (!targetPosition.isEmpty()) {
         PositionTracker depositTarget = targetPosition.get();
         Vec3 depositPosition = depositTarget.currentPosition();
         double distanceToTarget = depositPosition.distanceTo(body.getEyePosition());
         if (distanceToTarget < 3.0) {
            this.itemThrower.throwItem(level, body, depositTarget.currentPosition());
            body.getBrain().setMemory(this.cooldownMemory, this.cooldownDuration);
         }
      }
   }

   private boolean canThrowItemToTarget(final E body) {
      return this.hasItemPredicate.test(body) && this.targetPositionGetter.apply(body).isPresent();
   }

   @FunctionalInterface
   public interface ItemThrower<E> {
      void throwItem(ServerLevel level, E thrower, Vec3 targetPos);
   }
}
