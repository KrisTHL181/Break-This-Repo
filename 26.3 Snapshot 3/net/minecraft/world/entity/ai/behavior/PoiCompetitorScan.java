package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class PoiCompetitorScan {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         i -> i.group(i.present(MemoryModuleType.JOB_SITE), i.present(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            .apply(
               i,
               (jobSite, nearestEntities) -> (level, body, timestamp) -> {
                  GlobalPos pos = i.get(jobSite);
                  level.getPoiManager()
                     .getType(pos.pos())
                     .ifPresent(
                        poiType -> i.<List<LivingEntity>>get(nearestEntities)
                           .stream()
                           .filter(v -> v instanceof Villager && v != body)
                           .map(v -> (Villager)v)
                           .filter(LivingEntity::isAlive)
                           .filter(nearbyVillager -> competesForSameJobsite(pos, poiType, nearbyVillager))
                           .reduce(body, PoiCompetitorScan::selectWinner)
                     );
                  return true;
               }
            )
      );
   }

   private static Villager selectWinner(final Villager first, final Villager second) {
      Villager winner;
      Villager loser;
      if (first.getVillagerXp() > second.getVillagerXp()) {
         winner = first;
         loser = second;
      } else {
         winner = second;
         loser = first;
      }

      loser.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      return winner;
   }

   private static boolean competesForSameJobsite(final GlobalPos pos, final Holder<PoiType> poiType, final Villager nearbyVillager) {
      Optional<GlobalPos> jobSite = nearbyVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      return jobSite.isPresent() && pos.equals(jobSite.get()) && hasMatchingProfession(poiType, nearbyVillager.getVillagerData().profession());
   }

   private static boolean hasMatchingProfession(final Holder<PoiType> poiType, final Holder<VillagerProfession> profession) {
      return profession.value().heldJobSite().test(poiType);
   }
}
