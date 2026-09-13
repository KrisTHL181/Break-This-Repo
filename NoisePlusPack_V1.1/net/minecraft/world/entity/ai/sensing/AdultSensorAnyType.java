package net.minecraft.world.entity.ai.sensing;

import java.util.Optional;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class AdultSensorAnyType extends AdultSensor {
   @Override
   protected void setNearestVisibleAdult(LivingEntity p_409460_, NearestVisibleLivingEntities p_406741_) {
      Optional<LivingEntity> optional = p_406741_.findClosest(
         p_449603_ -> p_449603_.getType().is(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS) && !p_449603_.isBaby()
      );
      p_409460_.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
   }
}
