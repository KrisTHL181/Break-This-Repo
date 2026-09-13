package net.minecraft.world.entity.npc.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;

public interface VillagerDataHolder {
   VillagerData getVillagerData();

   void setVillagerData(VillagerData data);

   boolean getVillagerDataFinalized();

   void setVillagerDataFinalized(boolean villagerDataFinalized);

   default void finalizeVillagerType(final ServerLevelAccessor level, final BlockPos pos) {
      if (!this.getVillagerDataFinalized()) {
         this.setVillagerData(this.getVillagerData().withType(level.registryAccess(), VillagerType.byBiome(level.getBiome(pos))));
         this.setVillagerDataFinalized(true);
      }
   }
}
