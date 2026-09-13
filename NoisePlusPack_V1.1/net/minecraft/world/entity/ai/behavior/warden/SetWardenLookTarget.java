package net.minecraft.world.entity.ai.behavior.warden;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SetWardenLookTarget {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_258946_ -> p_258946_.group(
               p_258946_.registered(MemoryModuleType.LOOK_TARGET),
               p_258946_.registered(MemoryModuleType.DISTURBANCE_LOCATION),
               p_258946_.registered(MemoryModuleType.ROAR_TARGET),
               p_258946_.absent(MemoryModuleType.ATTACK_TARGET)
            )
            .apply(p_258946_, (p_258942_, p_258943_, p_258944_, p_258945_) -> (p_258936_, p_258937_, p_258938_) -> {
               Optional<BlockPos> optional = p_258946_.<LivingEntity>tryGet(p_258944_).map(Entity::blockPosition).or(() -> p_258946_.tryGet(p_258943_));
               if (optional.isEmpty()) {
                  return false;
               }

               p_258942_.set(new BlockPosTracker(optional.get()));
               return true;
            })
      );
   }
}
