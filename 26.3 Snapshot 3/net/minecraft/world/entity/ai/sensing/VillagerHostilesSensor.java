package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class VillagerHostilesSensor extends NearestVisibleLivingEntitySensor {
   private static final ImmutableMap<EntityType<?>, Float> ACCEPTABLE_DISTANCE_FROM_HOSTILES = ImmutableMap.builder()
      .put(EntityTypes.DROWNED, 8.0F)
      .put(EntityTypes.EVOKER, 12.0F)
      .put(EntityTypes.HUSK, 8.0F)
      .put(EntityTypes.ILLUSIONER, 12.0F)
      .put(EntityTypes.PILLAGER, 15.0F)
      .put(EntityTypes.RAVAGER, 12.0F)
      .put(EntityTypes.VEX, 8.0F)
      .put(EntityTypes.VINDICATOR, 10.0F)
      .put(EntityTypes.ZOGLIN, 10.0F)
      .put(EntityTypes.ZOMBIE, 8.0F)
      .put(EntityTypes.ZOMBIE_VILLAGER, 8.0F)
      .build();

   @Override
   protected boolean isMatchingEntity(final ServerLevel level, final LivingEntity body, final LivingEntity mob) {
      return this.isHostile(mob) && this.isClose(body, mob);
   }

   private boolean isClose(final LivingEntity body, final LivingEntity mob) {
      float distThreshold = (Float)ACCEPTABLE_DISTANCE_FROM_HOSTILES.get(mob.getType());
      return mob.distanceToSqr(body) <= distThreshold * distThreshold;
   }

   @Override
   protected MemoryModuleType<LivingEntity> getMemoryToSet() {
      return MemoryModuleType.NEAREST_HOSTILE;
   }

   private boolean isHostile(final LivingEntity entity) {
      return ACCEPTABLE_DISTANCE_FROM_HOSTILES.containsKey(entity.getType());
   }
}
