package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;

public class PiglinBruteSpecificSensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEARBY_ADULT_PIGLINS);
   }

   @Override
   protected void doTick(final ServerLevel level, final LivingEntity body) {
      Brain<?> brain = body.getBrain();
      NearestVisibleLivingEntities visibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .orElse(NearestVisibleLivingEntities.empty());
      Optional<Mob> nemesis = visibleLivingEntities.findClosest(entity -> entity instanceof WitherSkeleton || entity instanceof WitherBoss)
         .map(Mob.class::cast);
      List<AbstractPiglin> adultPiglins = PiglinAi.findNearbyAdultPiglins(brain);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nemesis);
      brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, adultPiglins);
   }
}
