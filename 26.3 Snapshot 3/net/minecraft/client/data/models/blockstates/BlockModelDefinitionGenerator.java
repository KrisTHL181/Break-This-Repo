package net.minecraft.client.data.models.blockstates;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.world.level.block.Block;

public interface BlockModelDefinitionGenerator {
   Block block();

   BlockStateModelDispatcher create();
}
