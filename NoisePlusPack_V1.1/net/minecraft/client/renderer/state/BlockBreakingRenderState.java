package net.minecraft.client.renderer.state;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockBreakingRenderState extends MovingBlockRenderState {
   public int progress;

   public BlockBreakingRenderState(ClientLevel p_430442_, BlockPos p_428491_, int p_425329_) {
      this.level = p_430442_;
      this.blockPos = p_428491_;
      this.blockState = p_430442_.getBlockState(p_428491_);
      this.progress = p_425329_;
      this.biome = p_430442_.getBiome(p_428491_);
   }
}
