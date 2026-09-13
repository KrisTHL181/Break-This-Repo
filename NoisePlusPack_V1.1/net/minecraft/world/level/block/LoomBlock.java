package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class LoomBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<LoomBlock> CODEC = simpleCodec(LoomBlock::new);
   private static final Component CONTAINER_TITLE = Component.translatable("container.loom");

   @Override
   public MapCodec<LoomBlock> codec() {
      return CODEC;
   }

   protected LoomBlock(BlockBehaviour.Properties p_54777_) {
      super(p_54777_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_54787_, Level p_54788_, BlockPos p_54789_, Player p_54790_, BlockHitResult p_54792_) {
      if (!p_54788_.isClientSide()) {
         p_54790_.openMenu(p_54787_.getMenuProvider(p_54788_, p_54789_));
         p_54790_.awardStat(Stats.INTERACT_WITH_LOOM);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   protected MenuProvider getMenuProvider(BlockState p_54796_, Level p_54797_, BlockPos p_54798_) {
      return new SimpleMenuProvider(
         (p_54783_, p_54784_, p_54785_) -> new LoomMenu(p_54783_, p_54784_, ContainerLevelAccess.create(p_54797_, p_54798_)), CONTAINER_TITLE
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54779_) {
      return this.defaultBlockState().setValue(FACING, p_54779_.getHorizontalDirection().getOpposite());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54794_) {
      p_54794_.add(FACING);
   }
}
