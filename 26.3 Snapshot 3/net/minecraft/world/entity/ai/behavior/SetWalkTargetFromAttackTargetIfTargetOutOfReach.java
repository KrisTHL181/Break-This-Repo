package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromAttackTargetIfTargetOutOfReach {
   private static final int PROJECTILE_ATTACK_RANGE_BUFFER = 1;

   public static BehaviorControl<Mob> create(final float speedModifier) {
      return create(mob -> speedModifier);
   }

   public static BehaviorControl<Mob> create(final Function<LivingEntity, Float> speedModifier) {
      return BehaviorBuilder.create(
         i -> i.group(
               i.registered(MemoryModuleType.WALK_TARGET),
               i.registered(MemoryModuleType.LOOK_TARGET),
               i.present(MemoryModuleType.ATTACK_TARGET),
               i.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(i, (walkTarget, lookTarget, attackTarget, nearestEntities) -> (level, body, timestamp) -> {
               LivingEntity toAttack = i.get(attackTarget);
               Optional<NearestVisibleLivingEntities> entities = i.tryGet(nearestEntities);
               if (entities.isPresent() && entities.get().contains(toAttack) && BehaviorUtils.isWithinAttackRange(body, toAttack, 1)) {
                  walkTarget.erase();
               } else {
                  lookTarget.set(new EntityTracker(toAttack, true));
                  walkTarget.set(new WalkTarget(new EntityTracker(toAttack, false), speedModifier.apply(body), 0));
               }

               return true;
            })
      );
   }
}
