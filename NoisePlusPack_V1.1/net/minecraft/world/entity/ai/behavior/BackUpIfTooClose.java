package net.minecraft.world.entity.ai.behavior;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class BackUpIfTooClose {
   public static OneShot<Mob> create(int p_259782_, float p_259071_) {
      return BehaviorBuilder.create(
         p_260278_ -> p_260278_.group(
               p_260278_.absent(MemoryModuleType.WALK_TARGET),
               p_260278_.registered(MemoryModuleType.LOOK_TARGET),
               p_260278_.present(MemoryModuleType.ATTACK_TARGET),
               p_260278_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(p_260278_, (p_260206_, p_259953_, p_259993_, p_259209_) -> (p_259617_, p_260038_, p_259374_) -> {
               LivingEntity livingentity = p_260278_.get(p_259993_);
               if (livingentity.closerThan(p_260038_, p_259782_) && p_260278_.<NearestVisibleLivingEntities>get(p_259209_).contains(livingentity)) {
                  p_259953_.set(new EntityTracker(livingentity, true));
                  p_260038_.getMoveControl().strafe(-p_259071_, 0.0F);
                  p_260038_.setYRot(Mth.rotateIfNecessary(p_260038_.getYRot(), p_260038_.yHeadRot, 0.0F));
                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
