package net.minecraft.world.item;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BedItem extends BlockItem {
    public BedItem(final Block block, final Item.Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(final BlockPlaceContext context, final BlockState placementState) {
        return context.getLevel().setBlock(context.getClickedPos(), placementState, 26);
    }
}