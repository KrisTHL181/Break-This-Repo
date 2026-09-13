package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.npc.villager.VillagerData;
import org.jspecify.annotations.Nullable;

public class ZombieVillagerRenderState extends ZombieRenderState implements VillagerDataHolderRenderState {
   public @Nullable VillagerData villagerData;

   @Override
   public @Nullable VillagerData getVillagerData() {
      return this.villagerData;
   }
}
