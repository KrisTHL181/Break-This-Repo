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
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SmithingTableBlock extends CraftingTableBlock {
   public static final MapCodec<SmithingTableBlock> CODEC = simpleCodec(SmithingTableBlock::new);
   private static final Component CONTAINER_TITLE = Component.translatable("container.upgrade");

   @Override
   public MapCodec<SmithingTableBlock> codec() {
      return CODEC;
   }

   protected SmithingTableBlock(BlockBehaviour.Properties p_56420_) {
      super(p_56420_);
   }

   @Override
   protected MenuProvider getMenuProvider(BlockState p_56435_, Level p_56436_, BlockPos p_56437_) {
      return new SimpleMenuProvider(
         (p_277304_, p_277305_, p_277306_) -> new SmithingMenu(p_277304_, p_277305_, ContainerLevelAccess.create(p_56436_, p_56437_)), CONTAINER_TITLE
      );
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_56428_, Level p_56429_, BlockPos p_56430_, Player p_56431_, BlockHitResult p_56433_) {
      if (!p_56429_.isClientSide()) {
         p_56431_.openMenu(p_56428_.getMenuProvider(p_56429_, p_56430_));
         p_56431_.awardStat(Stats.INTERACT_WITH_SMITHING_TABLE);
      }

      return InteractionResult.SUCCESS;
   }
}
