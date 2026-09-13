package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class Mount {
   private static final int CLOSE_ENOUGH_TO_START_RIDING_DIST = 1;

   public static BehaviorControl<LivingEntity> create(float p_259363_) {
      return BehaviorBuilder.create(
         p_259880_ -> p_259880_.group(
               p_259880_.registered(MemoryModuleType.LOOK_TARGET),
               p_259880_.absent(MemoryModuleType.WALK_TARGET),
               p_259880_.present(MemoryModuleType.RIDE_TARGET)
            )
            .apply(p_259880_, (p_259095_, p_260097_, p_259784_) -> (p_259242_, p_260257_, p_259083_) -> {
               if (p_260257_.isPassenger()) {
                  return false;
               }

               Entity entity = p_259880_.get(p_259784_);
               if (entity.closerThan(p_260257_, 1.0)) {
                  p_260257_.startRiding(entity);
               } else {
                  p_259095_.set(new EntityTracker(entity, true));
                  p_260097_.set(new WalkTarget(new EntityTracker(entity, false), p_259363_, 1));
               }

               return true;
            })
      );
   }
}
