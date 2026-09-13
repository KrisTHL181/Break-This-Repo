package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.apache.commons.lang3.mutable.MutableInt;

public class SetHiddenState {
   private static final int HIDE_TIMEOUT = 300;

   public static BehaviorControl<LivingEntity> create(int p_259244_, int p_260263_) {
      int i = p_259244_ * 20;
      MutableInt mutableint = new MutableInt(0);
      return BehaviorBuilder.create(
         p_259055_ -> p_259055_.group(p_259055_.present(MemoryModuleType.HIDING_PLACE), p_259055_.present(MemoryModuleType.HEARD_BELL_TIME))
            .apply(p_259055_, (p_260296_, p_260145_) -> (p_449551_, p_449552_, p_449553_) -> {
               long j = p_259055_.<Long>get(p_260145_);
               boolean flag = j + 300L <= p_449553_;
               if (mutableint.intValue() <= i && !flag) {
                  BlockPos blockpos = p_259055_.<GlobalPos>get(p_260296_).pos();
                  if (blockpos.closerThan(p_449552_.blockPosition(), p_260263_)) {
                     mutableint.increment();
                  }

                  return true;
               } else {
                  p_260145_.erase();
                  p_260296_.erase();
                  p_449552_.getBrain().updateActivityFromSchedule(p_449551_.environmentAttributes(), p_449551_.getGameTime(), p_449552_.position());
                  mutableint.setValue(0);
                  return true;
               }
            })
      );
   }
}
