package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class StopBeingAngryIfTargetDead {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         p_258814_ -> p_258814_.group(p_258814_.present(MemoryModuleType.ANGRY_AT))
            .apply(
               p_258814_,
               p_258813_ -> (p_421723_, p_421724_, p_421725_) -> {
                  Optional.ofNullable(p_421723_.getEntity(p_258814_.get(p_258813_)))
                     .map(p_258802_ -> p_258802_ instanceof LivingEntity livingentity ? livingentity : null)
                     .filter(LivingEntity::isDeadOrDying)
                     .filter(p_449562_ -> p_449562_.getType() != EntityType.PLAYER || p_421723_.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS))
                     .ifPresent(p_258811_ -> p_258813_.erase());
                  return true;
               }
            )
      );
   }
}
