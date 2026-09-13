package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BecomePassiveIfMemoryPresent {
   public static BehaviorControl<LivingEntity> create(final MemoryModuleType<?> pacifyingMemory, final int pacifyDuration) {
      return BehaviorBuilder.create(
         i -> i.group(i.registered(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.PACIFIED), i.present(pacifyingMemory))
            .apply(i, i.point(() -> "[BecomePassive if " + pacifyingMemory + " present]", (attackTarget, pacified, pacifying) -> (level, body, timestamp) -> {
               pacified.setWithExpiry(true, pacifyDuration);
               attackTarget.erase();
               return true;
            }))
      );
   }
}
