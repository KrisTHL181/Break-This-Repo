package net.minecraft.world.entity.animal.axolotl;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class ValidatePlayDead {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_259464_ -> p_259464_.group(p_259464_.present(MemoryModuleType.PLAY_DEAD_TICKS), p_259464_.registered(MemoryModuleType.HURT_BY_ENTITY))
            .apply(p_259464_, (p_259173_, p_259591_) -> (p_260159_, p_259720_, p_259523_) -> {
               int i = p_259464_.<Integer>get(p_259173_);
               if (i <= 0) {
                  p_259173_.erase();
                  p_259591_.erase();
                  p_259720_.getBrain().useDefaultActivity();
               } else {
                  p_259173_.set(i - 1);
               }

               return true;
            })
      );
   }
}
