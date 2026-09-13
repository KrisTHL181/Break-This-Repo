package net.minecraft.world.entity.ai.sensing;

import java.util.Optional;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class AdultSensorAnyType extends AdultSensor {
   @Override
   protected void setNearestVisibleAdult(final LivingEntity body, final NearestVisibleLivingEntities visibleLivingEntities) {
      Optional<LivingEntity> adult = visibleLivingEntities.findClosest(entity -> entity.is(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS) && !entity.isBaby());
      body.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, adult);
   }
}
