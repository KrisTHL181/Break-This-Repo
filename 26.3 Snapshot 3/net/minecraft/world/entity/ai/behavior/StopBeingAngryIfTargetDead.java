package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class StopBeingAngryIfTargetDead {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         i -> i.group(i.present(MemoryModuleType.ANGRY_AT))
            .apply(
               i,
               angryAt -> (level, body, timestamp) -> {
                  Optional.ofNullable(level.getEntity(i.get(angryAt)))
                     .map(entity -> entity instanceof LivingEntity livingEntity ? livingEntity : null)
                     .filter(LivingEntity::isDeadOrDying)
                     .filter(angerTarget -> !angerTarget.is(EntityTypes.PLAYER) || level.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS))
                     .ifPresent(angerTarget -> angryAt.erase());
                  return true;
               }
            )
      );
   }
}
