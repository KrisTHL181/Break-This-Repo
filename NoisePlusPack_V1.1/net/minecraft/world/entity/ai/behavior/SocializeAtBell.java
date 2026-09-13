package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SocializeAtBell {
   private static final float SPEED_MODIFIER = 0.3F;

   public static OneShot<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_258755_ -> p_258755_.group(
               p_258755_.registered(MemoryModuleType.WALK_TARGET),
               p_258755_.registered(MemoryModuleType.LOOK_TARGET),
               p_258755_.present(MemoryModuleType.MEETING_POINT),
               p_258755_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES),
               p_258755_.absent(MemoryModuleType.INTERACTION_TARGET)
            )
            .apply(
               p_258755_,
               (p_258750_, p_258751_, p_258752_, p_258753_, p_258754_) -> (p_258766_, p_258767_, p_258768_) -> {
                  GlobalPos globalpos = p_258755_.get(p_258752_);
                  NearestVisibleLivingEntities nearestvisiblelivingentities = p_258755_.get(p_258753_);
                  if (p_258766_.getRandom().nextInt(100) == 0
                     && p_258766_.dimension() == globalpos.dimension()
                     && globalpos.pos().closerToCenterThan(p_258767_.position(), 4.0)
                     && nearestvisiblelivingentities.contains(p_449560_ -> EntityType.VILLAGER.equals(p_449560_.getType()))) {
                     nearestvisiblelivingentities.findClosest(
                           p_449559_ -> EntityType.VILLAGER.equals(p_449559_.getType()) && p_449559_.distanceToSqr(p_258767_) <= 32.0
                        )
                        .ifPresent(p_258759_ -> {
                           p_258754_.set(p_258759_);
                           p_258751_.set(new EntityTracker(p_258759_, true));
                           p_258750_.set(new WalkTarget(new EntityTracker(p_258759_, false), 0.3F, 1));
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
