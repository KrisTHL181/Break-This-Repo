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
         p_258576_ -> p_258576_.group(p_258576_.present(MemoryModuleType.JOB_SITE), p_258576_.present(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            .apply(
               p_258576_,
               (p_258590_, p_258591_) -> (p_258580_, p_454576_, p_258582_) -> {
                  GlobalPos globalpos = p_258576_.get(p_258590_);
                  p_258580_.getPoiManager()
                     .getType(globalpos.pos())
                     .ifPresent(
                        p_258588_ -> p_258576_.<List<LivingEntity>>get(p_258591_)
                           .stream()
                           .filter(p_449525_ -> p_449525_ instanceof Villager && p_449525_ != p_454576_)
                           .map(p_449526_ -> (Villager)p_449526_)
                           .filter(LivingEntity::isAlive)
                           .filter(p_449523_ -> competesForSameJobsite(globalpos, p_258588_, p_449523_))
                           .reduce(p_454576_, PoiCompetitorScan::selectWinner)
                     );
                  return true;
               }
            )
      );
   }

   private static Villager selectWinner(Villager p_455706_, Villager p_460508_) {
      Villager villager;
      Villager villager1;
      if (p_455706_.getVillagerXp() > p_460508_.getVillagerXp()) {
         villager = p_455706_;
         villager1 = p_460508_;
      } else {
         villager = p_460508_;
         villager1 = p_455706_;
      }

      villager1.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      return villager;
   }

   private static boolean competesForSameJobsite(GlobalPos p_217330_, Holder<PoiType> p_217331_, Villager p_460689_) {
      Optional<GlobalPos> optional = p_460689_.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      return optional.isPresent() && p_217330_.equals(optional.get()) && hasMatchingProfession(p_217331_, p_460689_.getVillagerData().profession());
   }

   private static boolean hasMatchingProfession(Holder<PoiType> p_217334_, Holder<VillagerProfession> p_394408_) {
      return p_394408_.value().heldJobSite().test(p_217334_);
   }
}
