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
   protected void doTick(ServerLevel p_26710_, T p_26711_) {
      double d0 = p_26711_.getAttributeValue(Attributes.FOLLOW_RANGE);
      AABB aabb = p_26711_.getBoundingBox().inflate(d0, d0, d0);
      List<LivingEntity> list = p_26710_.getEntitiesOfClass(LivingEntity.class, aabb, p_26717_ -> p_26717_ != p_26711_ && p_26717_.isAlive());
      list.sort(Comparator.comparingDouble(p_26711_::distanceToSqr));
      Brain<?> brain = p_26711_.getBrain();
      brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, list);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(p_26710_, p_26711_, list));
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}
