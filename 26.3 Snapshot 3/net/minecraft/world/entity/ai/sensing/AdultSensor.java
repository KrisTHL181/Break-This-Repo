package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class AdultSensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }

   @Override
   protected void doTick(final ServerLevel level, final LivingEntity body) {
      body.getBrain()
         .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .ifPresent(livingEntities -> this.setNearestVisibleAdult(body, livingEntities));
   }

   protected void setNearestVisibleAdult(final LivingEntity body, final NearestVisibleLivingEntities visibleLivingEntities) {
      Optional<LivingEntity> adult = visibleLivingEntities.findClosest(entity -> entity.getType() == body.getType() && !entity.isBaby());
      body.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, adult);
   }
}
