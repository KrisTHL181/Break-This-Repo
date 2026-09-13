package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

public class NearestLivingEntitySensor<T extends LivingEntity> extends Sensor<T> {
   @Override
   protected void doTick(final ServerLevel level, final T body) {
      double followRange = body.getAttributeValue(Attributes.FOLLOW_RANGE);
      AABB boundingBox = body.getBoundingBox().inflate(followRange, followRange, followRange);
      List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, mob -> mob != body && mob.isAlive());
      livingEntities.sort(Comparator.comparingDouble(body::distanceToSqr));
      Brain<?> brain = body.getBrain();
      brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, livingEntities);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(level, body, livingEntities));
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}
