package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;

public class HoglinSpecificSensor extends Sensor<Hoglin> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(
         MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
         MemoryModuleType.NEAREST_REPELLENT,
         MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN,
         MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS,
         MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
         MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
         new MemoryModuleType[0]
      );
   }

   protected void doTick(final ServerLevel level, final Hoglin body) {
      Brain<?> brain = body.getBrain();
      brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, this.findNearestRepellent(level, body));
      Optional<Piglin> adultPiglin = Optional.empty();
      int adultPiglinCount = 0;
      List<Hoglin> adultHoglins = Lists.newArrayList();
      NearestVisibleLivingEntities visibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .orElse(NearestVisibleLivingEntities.empty());

      for (LivingEntity entity : visibleLivingEntities.findAll(entityx -> !entityx.isBaby() && (entityx instanceof Piglin || entityx instanceof Hoglin))) {
         if (entity instanceof Piglin piglin) {
            adultPiglinCount++;
            if (adultPiglin.isEmpty()) {
               adultPiglin = Optional.of(piglin);
            }
         }

         if (entity instanceof Hoglin hoglin) {
            adultHoglins.add(hoglin);
         }
      }

      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, adultPiglin);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, adultHoglins);
      brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, adultPiglinCount);
      brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, adultHoglins.size());
   }

   private Optional<BlockPos> findNearestRepellent(final ServerLevel level, final Hoglin body) {
      return BlockPos.findClosestMatch(body.blockPosition(), 8, 4, pos -> level.getBlockState(pos).is(BlockTags.HOGLIN_REPELLENTS));
   }
}
