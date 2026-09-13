package net.minecraft.world.entity.ai.sensing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.frog.Frog;

public class FrogAttackablesSensor extends NearestVisibleLivingEntitySensor {
   public static final float TARGET_DETECTION_DISTANCE = 10.0F;

   @Override
   protected boolean isMatchingEntity(ServerLevel p_364007_, LivingEntity p_217810_, LivingEntity p_217811_) {
      return !p_217810_.getBrain().hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN)
            && Sensor.isEntityAttackable(p_364007_, p_217810_, p_217811_)
            && Frog.canEat(p_217811_)
            && !this.isUnreachableAttackTarget(p_217810_, p_217811_)
         ? p_217811_.closerThan(p_217810_, 10.0)
         : false;
   }

   private boolean isUnreachableAttackTarget(LivingEntity p_238336_, LivingEntity p_238337_) {
      List<UUID> list = p_238336_.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
      return list.contains(p_238337_.getUUID());
   }

   @Override
   protected MemoryModuleType<LivingEntity> getMemory() {
      return MemoryModuleType.NEAREST_ATTACKABLE;
   }
}
