package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromLookTarget {
   public static OneShot<LivingEntity> create(float p_259702_, int p_259510_) {
      return create(p_182369_ -> true, p_182364_ -> p_259702_, p_259510_);
   }

   public static OneShot<LivingEntity> create(Predicate<LivingEntity> p_260341_, Function<LivingEntity, Float> p_260269_, int p_259192_) {
      return BehaviorBuilder.create(
         p_258748_ -> p_258748_.group(p_258748_.absent(MemoryModuleType.WALK_TARGET), p_258748_.present(MemoryModuleType.LOOK_TARGET))
            .apply(p_258748_, (p_258743_, p_258744_) -> (p_258736_, p_258737_, p_258738_) -> {
               if (!p_260341_.test(p_258737_)) {
                  return false;
               }

               p_258743_.set(new WalkTarget(p_258748_.get(p_258744_), p_260269_.apply(p_258737_), p_259192_));
               return true;
            })
      );
   }
}
