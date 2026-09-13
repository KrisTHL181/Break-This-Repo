package net.minecraft.world.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public record ActivityData<E extends LivingEntity>(
   Activity activityType,
   ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs,
   Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions,
   Set<MemoryModuleType<?>> memoriesToEraseWhenStopped
) {
   public static <E extends LivingEntity> ActivityData<E> create(
      final Activity activity, final int priorityOfFirstBehavior, final ImmutableList<? extends BehaviorControl<? super E>> behaviorList
   ) {
      return create(activity, createPriorityPairs(priorityOfFirstBehavior, behaviorList));
   }

   public static <E extends LivingEntity> ActivityData<E> create(
      final Activity activity,
      final int priorityOfFirstBehavior,
      final ImmutableList<? extends BehaviorControl<? super E>> behaviorList,
      final MemoryModuleType<?> memoryThatMustHaveValueAndWillBeErasedAfter
   ) {
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions = ImmutableSet.of(
         Pair.of(memoryThatMustHaveValueAndWillBeErasedAfter, MemoryStatus.VALUE_PRESENT)
      );
      Set<MemoryModuleType<?>> memoriesToEraseWhenStopped = ImmutableSet.of(memoryThatMustHaveValueAndWillBeErasedAfter);
      return create(activity, createPriorityPairs(priorityOfFirstBehavior, behaviorList), conditions, memoriesToEraseWhenStopped);
   }

   public static <E extends LivingEntity> ActivityData<E> create(
      final Activity activity, final ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs
   ) {
      return create(activity, behaviorPriorityPairs, ImmutableSet.of(), Sets.newHashSet());
   }

   public static <E extends LivingEntity> ActivityData<E> create(
      final Activity activity,
      final int priorityOfFirstBehavior,
      final ImmutableList<? extends BehaviorControl<? super E>> behaviorList,
      final Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions
   ) {
      return create(activity, createPriorityPairs(priorityOfFirstBehavior, behaviorList), conditions);
   }

   public static <E extends LivingEntity> ActivityData<E> create(
      final Activity activity,
      final ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs,
      final Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions
   ) {
      return create(activity, behaviorPriorityPairs, conditions, Sets.newHashSet());
   }

   public static <E extends LivingEntity> ActivityData<E> create(
      final Activity activity,
      final ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs,
      final Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions,
      final Set<MemoryModuleType<?>> memoriesToEraseWhenStopped
   ) {
      return new ActivityData<>(activity, behaviorPriorityPairs, conditions, memoriesToEraseWhenStopped);
   }

   public static <E extends LivingEntity> ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> createPriorityPairs(
      final int priorityOfFirstBehavior, final ImmutableList<? extends BehaviorControl<? super E>> behaviorList
   ) {
      int nextPrio = priorityOfFirstBehavior;
      Builder<Pair<Integer, ? extends BehaviorControl<? super E>>> listBuilder = ImmutableList.builder();
      UnmodifiableIterator var4 = behaviorList.iterator();

      while (var4.hasNext()) {
         BehaviorControl<? super E> behavior = (BehaviorControl<? super E>)var4.next();
         listBuilder.add(Pair.of(nextPrio++, behavior));
      }

      return listBuilder.build();
   }
}
