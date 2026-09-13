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
         i -> i.group(
               i.registered(MemoryModuleType.LOOK_TARGET),
               i.registered(MemoryModuleType.DISTURBANCE_LOCATION),
               i.registered(MemoryModuleType.ROAR_TARGET),
               i.absent(MemoryModuleType.ATTACK_TARGET)
            )
            .apply(i, (lookTarget, disturbance, roarTarget, attackTarget) -> (level, body, timestamp) -> {
               Optional<BlockPos> target = i.<LivingEntity>tryGet(roarTarget).map(Entity::blockPosition).or(() -> i.tryGet(disturbance));
               if (target.isEmpty()) {
                  return false;
               }

               lookTarget.set(new BlockPosTracker(target.get()));
               return true;
            })
      );
   }
}
