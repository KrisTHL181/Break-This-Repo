package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class VillagerBabiesSensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
   }

   @Override
   protected void doTick(final ServerLevel level, final LivingEntity body) {
      body.getBrain().setMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES, this.getNearestVillagerBabies(body));
   }

   private List<LivingEntity> getNearestVillagerBabies(final LivingEntity myBody) {
      return ImmutableList.copyOf(this.getVisibleEntities(myBody).findAll(this::isVillagerBaby));
   }

   private boolean isVillagerBaby(final LivingEntity entity) {
      return entity.is(EntityTypes.VILLAGER) && entity.isBaby();
   }

   private NearestVisibleLivingEntities getVisibleEntities(final LivingEntity myBody) {
      return myBody.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());
   }
}
