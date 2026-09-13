package net.minecraft.world.entity.ai.sensing;

import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public abstract class NearestVisibleLivingEntitySensor extends Sensor<LivingEntity> {
   protected abstract boolean isMatchingEntity(final ServerLevel level, LivingEntity body, LivingEntity mob);

   protected abstract MemoryModuleType<LivingEntity> getMemoryToSet();

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return Set.of(this.getMemoryToSet(), MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }

   @Override
   protected void doTick(final ServerLevel level, final LivingEntity body) {
      body.getBrain().setMemory(this.getMemoryToSet(), this.getNearestEntity(level, body));
   }

   private Optional<LivingEntity> getNearestEntity(final ServerLevel level, final LivingEntity body) {
      return this.getVisibleEntities(body).flatMap(livingEntities -> livingEntities.findClosest(mob -> this.isMatchingEntity(level, body, mob)));
   }

   protected Optional<NearestVisibleLivingEntities> getVisibleEntities(final LivingEntity body) {
      return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}
