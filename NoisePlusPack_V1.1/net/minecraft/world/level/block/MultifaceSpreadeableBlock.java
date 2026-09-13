package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class MultifaceSpreadeableBlock extends MultifaceBlock {
   public MultifaceSpreadeableBlock(BlockBehaviour.Properties p_377832_) {
      super(p_377832_);
   }

   @Override
   public abstract MapCodec<? extends MultifaceSpreadeableBlock> codec();

   public abstract MultifaceSpreader getSpreader();
}
