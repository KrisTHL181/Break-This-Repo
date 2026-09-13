package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ZombieVillagerRenderState extends ZombieRenderState implements VillagerDataHolderRenderState {
    public @Nullable VillagerData villagerData;

    @Override
    public @Nullable VillagerData getVillagerData() {
        return this.villagerData;
    }
}