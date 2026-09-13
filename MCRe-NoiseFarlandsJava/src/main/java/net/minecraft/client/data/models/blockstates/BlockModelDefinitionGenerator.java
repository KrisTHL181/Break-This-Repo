package net.minecraft.client.data.models.blockstates;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BlockModelDefinitionGenerator {
    Block block();

    BlockStateModelDispatcher create();
}