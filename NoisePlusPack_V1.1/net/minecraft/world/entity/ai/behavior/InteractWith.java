package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith {
   public static <T extends LivingEntity> BehaviorControl<LivingEntity> of(
      EntityType<? extends T> p_259703_, int p_260224_, MemoryModuleType<T> p_259995_, float p_259991_, int p_259933_
   ) {
      return of(p_259703_, p_260224_, p_23287_ -> true, p_23285_ -> true, p_259995_, p_259991_, p_259933_);
   }

   public static <E extends LivingEntity, T extends LivingEntity> BehaviorControl<E> of(
      EntityType<? extends T> p_259366_,
      int p_259564_,
      Predicate<E> p_259570_,
      Predicate<T> p_260254_,
      MemoryModuleType<T> p_260229_,
      float p_259369_,
      int p_259065_
   ) {
      int i = p_259564_ * p_259564_;
      Predicate<LivingEntity> predicate = p_449480_ -> p_259366_.equals(p_449480_.getType()) && p_260254_.test((T)p_449480_);
      return BehaviorBuilder.create(
         p_258426_ -> p_258426_.group(
               p_258426_.registered(p_260229_),
               p_258426_.registered(MemoryModuleType.LOOK_TARGET),
               p_258426_.absent(MemoryModuleType.WALK_TARGET),
               p_258426_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               p_258426_,
               (p_258439_, p_258440_, p_258441_, p_258442_) -> (p_258413_, p_258414_, p_258415_) -> {
                  NearestVisibleLivingEntities nearestvisiblelivingentities = p_258426_.get(p_258442_);
                  if (p_259570_.test((E)p_258414_) && nearestvisiblelivingentities.contains(predicate)) {
                     Optional<LivingEntity> optional = nearestvisiblelivingentities.findClosest(
                        p_326832_ -> p_326832_.distanceToSqr(p_258414_) <= i && predicate.test(p_326832_)
                     );
                     optional.ifPresent(p_258432_ -> {
                        p_258439_.set(p_258432_);
                        p_258440_.set(new EntityTracker(p_258432_, true));
                        p_258441_.set(new WalkTarget(new EntityTracker(p_258432_, false), p_259369_, p_259065_));
                     });
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}
