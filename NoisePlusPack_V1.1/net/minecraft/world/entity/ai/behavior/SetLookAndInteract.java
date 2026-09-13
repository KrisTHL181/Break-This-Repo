package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetLookAndInteract {
   public static BehaviorControl<LivingEntity> create(EntityType<?> p_259642_, int p_259805_) {
      int i = p_259805_ * p_259805_;
      return BehaviorBuilder.create(
         p_258685_ -> p_258685_.group(
               p_258685_.registered(MemoryModuleType.LOOK_TARGET),
               p_258685_.absent(MemoryModuleType.INTERACTION_TARGET),
               p_258685_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               p_258685_,
               (p_258680_, p_258681_, p_258682_) -> (p_258670_, p_258671_, p_258672_) -> {
                  Optional<LivingEntity> optional = p_258685_.<NearestVisibleLivingEntities>get(p_258682_)
                     .findClosest(p_449557_ -> p_449557_.distanceToSqr(p_258671_) <= i && p_259642_.equals(p_449557_.getType()));
                  if (optional.isEmpty()) {
                     return false;
                  }

                  LivingEntity livingentity = optional.get();
                  p_258681_.set(livingentity);
                  p_258680_.set(new EntityTracker(livingentity, true));
                  return true;
               }
            )
      );
   }
}
