package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class WitherWallSkullBlock extends WallSkullBlock {
   public static final MapCodec<WitherWallSkullBlock> CODEC = simpleCodec(WitherWallSkullBlock::new);

   @Override
   public MapCodec<WitherWallSkullBlock> codec() {
      return CODEC;
   }

   protected WitherWallSkullBlock(BlockBehaviour.Properties p_58276_) {
      super(SkullBlock.Types.WITHER_SKELETON, p_58276_);
   }

   @Override
   public void setPlacedBy(Level p_58278_, BlockPos p_58279_, BlockState p_58280_, @Nullable LivingEntity p_58281_, ItemStack p_58282_) {
      WitherSkullBlock.checkSpawn(p_58278_, p_58279_);
   }
}
