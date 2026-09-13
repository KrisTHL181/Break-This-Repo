package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class StartCelebratingIfTargetDead {
   public static BehaviorControl<LivingEntity> create(int p_259238_, BiPredicate<LivingEntity, LivingEntity> p_259102_) {
      return BehaviorBuilder.create(
         p_259600_ -> p_259600_.group(
               p_259600_.present(MemoryModuleType.ATTACK_TARGET),
               p_259600_.registered(MemoryModuleType.ANGRY_AT),
               p_259600_.absent(MemoryModuleType.CELEBRATE_LOCATION),
               p_259600_.registered(MemoryModuleType.DANCING)
            )
            .apply(p_259600_, (p_259049_, p_259067_, p_259031_, p_259141_) -> (p_259956_, p_259611_, p_259619_) -> {
               LivingEntity livingentity = p_259600_.get(p_259049_);
               if (!livingentity.isDeadOrDying()) {
                  return false;
               }

               if (p_259102_.test(p_259611_, livingentity)) {
                  p_259141_.setWithExpiry(true, p_259238_);
               }

               p_259031_.setWithExpiry(livingentity.blockPosition(), p_259238_);
               if (livingentity.getType() != EntityType.PLAYER || p_259956_.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS)) {
                  p_259049_.erase();
                  p_259067_.erase();
               }

               return true;
            })
      );
   }
}
