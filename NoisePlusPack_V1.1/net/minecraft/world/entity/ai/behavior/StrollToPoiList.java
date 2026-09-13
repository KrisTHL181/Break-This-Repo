package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoiList {
   public static BehaviorControl<Villager> create(
      MemoryModuleType<List<GlobalPos>> p_259573_, float p_259895_, int p_260285_, int p_259533_, MemoryModuleType<GlobalPos> p_259706_
   ) {
      MutableLong mutablelong = new MutableLong(0L);
      return BehaviorBuilder.create(
         p_259612_ -> p_259612_.group(p_259612_.registered(MemoryModuleType.WALK_TARGET), p_259612_.present(p_259573_), p_259612_.present(p_259706_))
            .apply(p_259612_, (p_259574_, p_259801_, p_259116_) -> (p_259940_, p_455061_, p_260161_) -> {
               List<GlobalPos> list = p_259612_.get(p_259801_);
               GlobalPos globalpos = p_259612_.get(p_259116_);
               if (list.isEmpty()) {
                  return false;
               }

               GlobalPos globalpos1 = list.get(p_259940_.getRandom().nextInt(list.size()));
               if (globalpos1 != null && p_259940_.dimension() == globalpos1.dimension() && globalpos.pos().closerToCenterThan(p_455061_.position(), p_259533_)
                  )
                {
                  if (p_260161_ > mutablelong.longValue()) {
                     p_259574_.set(new WalkTarget(globalpos1.pos(), p_259895_, p_260285_));
                     mutablelong.setValue(p_260161_ + 100L);
                  }

                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
