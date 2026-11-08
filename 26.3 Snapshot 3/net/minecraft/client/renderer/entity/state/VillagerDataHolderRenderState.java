package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.npc.villager.VillagerData;
import org.jspecify.annotations.Nullable;

public interface VillagerDataHolderRenderState {
   @Nullable VillagerData getVillagerData();
}
