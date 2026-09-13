package net.minecraft.world.entity.ai.behavior.warden;

import net.minecraft.util.Unit;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class TryToSniff {
   private static final IntProvider SNIFF_COOLDOWN = UniformInt.of(100, 200);

   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         i -> i.group(
               i.registered(MemoryModuleType.IS_SNIFFING),
               i.registered(MemoryModuleType.WALK_TARGET),
               i.absent(MemoryModuleType.SNIFF_COOLDOWN),
               i.present(MemoryModuleType.NEAREST_ATTACKABLE),
               i.absent(MemoryModuleType.DISTURBANCE_LOCATION)
            )
            .apply(i, (sniffing, walkTarget, cooldown, attackable, disturbance) -> (level, body, timestamp) -> {
               sniffing.set(Unit.INSTANCE);
               cooldown.setWithExpiry(Unit.INSTANCE, SNIFF_COOLDOWN.sample(level.getRandom()));
               walkTarget.erase();
               body.setPose(Pose.SNIFFING);
               return true;
            })
      );
   }
}
