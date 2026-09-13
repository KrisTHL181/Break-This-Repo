package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class ResetProfession {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         p_259684_ -> p_259684_.group(p_259684_.absent(MemoryModuleType.JOB_SITE)).apply(p_259684_, p_260035_ -> (p_390572_, p_458130_, p_390574_) -> {
            VillagerData villagerdata = p_458130_.getVillagerData();
            boolean flag = !villagerdata.profession().is(VillagerProfession.NONE) && !villagerdata.profession().is(VillagerProfession.NITWIT);
            if (flag && p_458130_.getVillagerXp() == 0 && villagerdata.level() <= 1) {
               p_458130_.setVillagerData(p_458130_.getVillagerData().withProfession(p_390572_.registryAccess(), VillagerProfession.NONE));
               p_458130_.refreshBrain(p_390572_);
               return true;
            } else {
               return false;
            }
         })
      );
   }
}
