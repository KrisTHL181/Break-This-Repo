package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class AssignProfessionFromJobSite {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         p_258312_ -> p_258312_.group(p_258312_.present(MemoryModuleType.POTENTIAL_JOB_SITE), p_258312_.registered(MemoryModuleType.JOB_SITE))
            .apply(
               p_258312_,
               (p_258304_, p_258305_) -> (p_258309_, p_460076_, p_258311_) -> {
                  GlobalPos globalpos = p_258312_.get(p_258304_);
                  if (!globalpos.pos().closerToCenterThan(p_460076_.position(), 2.0) && !p_460076_.assignProfessionWhenSpawned()) {
                     return false;
                  }

                  p_258304_.erase();
                  p_258305_.set(globalpos);
                  p_258309_.broadcastEntityEvent(p_460076_, (byte)14);
                  if (!p_460076_.getVillagerData().profession().is(VillagerProfession.NONE)) {
                     return true;
                  }

                  MinecraftServer minecraftserver = p_258309_.getServer();
                  Optional.ofNullable(minecraftserver.getLevel(globalpos.dimension()))
                     .flatMap(p_22467_ -> p_22467_.getPoiManager().getType(globalpos.pos()))
                     .flatMap(
                        p_390545_ -> BuiltInRegistries.VILLAGER_PROFESSION
                           .listElements()
                           .filter(p_449460_ -> p_449460_.value().heldJobSite().test((Holder<PoiType>)p_390545_))
                           .findFirst()
                     )
                     .ifPresent(p_449458_ -> {
                        p_460076_.setVillagerData(p_460076_.getVillagerData().withProfession(p_449458_));
                        p_460076_.refreshBrain(p_258309_);
                     });
                  return true;
               }
            )
      );
   }
}
