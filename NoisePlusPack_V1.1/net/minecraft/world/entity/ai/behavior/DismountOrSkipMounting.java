package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DismountOrSkipMounting {
   public static <E extends LivingEntity> BehaviorControl<E> create(int p_259945_, BiPredicate<E, Entity> p_259837_) {
      return BehaviorBuilder.create(
         p_259780_ -> p_259780_.group(p_259780_.registered(MemoryModuleType.RIDE_TARGET)).apply(p_259780_, p_259326_ -> (p_259287_, p_259246_, p_259462_) -> {
            Entity entity = p_259246_.getVehicle();
            Entity entity1 = p_259780_.<Entity>tryGet(p_259326_).orElse(null);
            if (entity == null && entity1 == null) {
               return false;
            }

            Entity entity2 = entity == null ? entity1 : entity;
            if (isVehicleValid(p_259246_, entity2, p_259945_) && !p_259837_.test((E)p_259246_, entity2)) {
               return false;
            }

            p_259246_.stopRiding();
            p_259326_.erase();
            return true;
         })
      );
   }

   private static boolean isVehicleValid(LivingEntity p_259293_, Entity p_260023_, int p_259048_) {
      return p_260023_.isAlive() && p_260023_.closerThan(p_259293_, p_259048_) && p_260023_.level() == p_259293_.level();
   }
}
