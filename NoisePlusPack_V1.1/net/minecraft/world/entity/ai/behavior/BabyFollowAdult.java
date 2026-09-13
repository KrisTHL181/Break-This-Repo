package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
   public static OneShot<LivingEntity> create(UniformInt p_260109_, float p_259621_) {
      return create(p_260109_, p_147421_ -> p_259621_, MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
   }

   public static OneShot<LivingEntity> create(
      UniformInt p_259321_, Function<LivingEntity, Float> p_259190_, MemoryModuleType<? extends LivingEntity> p_410630_, boolean p_409512_
   ) {
      return BehaviorBuilder.create(
         p_405320_ -> p_405320_.group(
               p_405320_.present(p_410630_), p_405320_.registered(MemoryModuleType.LOOK_TARGET), p_405320_.absent(MemoryModuleType.WALK_TARGET)
            )
            .apply(
               p_405320_,
               (p_405313_, p_405314_, p_405315_) -> (p_258326_, p_408904_, p_258328_) -> {
                  if (!p_408904_.isBaby()) {
                     return false;
                  } else {
                     LivingEntity livingentity = p_405320_.get(p_405313_);
                     if (p_408904_.closerThan(livingentity, p_259321_.getMaxValue() + 1) && !p_408904_.closerThan(livingentity, p_259321_.getMinValue())) {
                        WalkTarget walktarget = new WalkTarget(
                           new EntityTracker(livingentity, p_409512_, p_409512_), p_259190_.apply(p_408904_), p_259321_.getMinValue() - 1
                        );
                        p_405314_.set(new EntityTracker(livingentity, true, p_409512_));
                        p_405315_.set(walktarget);
                        return true;
                     } else {
                        return false;
                     }
                  }
               }
            )
      );
   }
}
