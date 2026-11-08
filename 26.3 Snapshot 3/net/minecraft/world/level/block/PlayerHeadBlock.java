package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class PlayerHeadBlock extends SkullBlock {
   protected PlayerHeadBlock(final BlockBehaviour.Properties properties) {
      super(SkullBlock.Types.PLAYER, properties);
   }
}
