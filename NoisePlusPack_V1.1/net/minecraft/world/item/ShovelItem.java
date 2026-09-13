package net.minecraft.world.item;

import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class ShovelItem extends Item {
   protected static final Map<Block, BlockState> FLATTENABLES = Maps.newHashMap(
      new Builder()
         .put(Blocks.GRASS_BLOCK, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.DIRT, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.PODZOL, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.COARSE_DIRT, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.MYCELIUM, Blocks.DIRT_PATH.defaultBlockState())
         .put(Blocks.ROOTED_DIRT, Blocks.DIRT_PATH.defaultBlockState())
         .build()
   );

   public ShovelItem(ToolMaterial p_366398_, float p_361074_, float p_368875_, Item.Properties p_43117_) {
      super(p_43117_.shovel(p_366398_, p_361074_, p_368875_));
   }

   @Override
   public InteractionResult useOn(UseOnContext p_43119_) {
      Level level = p_43119_.getLevel();
      BlockPos blockpos = p_43119_.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      if (p_43119_.getClickedFace() == Direction.DOWN) {
         return InteractionResult.PASS;
      }

      Player player = p_43119_.getPlayer();
      BlockState blockstate1 = FLATTENABLES.get(blockstate.getBlock());
      BlockState blockstate2 = null;
      if (blockstate1 != null && level.getBlockState(blockpos.above()).isAir()) {
         level.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
         blockstate2 = blockstate1;
      } else if (blockstate.getBlock() instanceof CampfireBlock && blockstate.getValue(CampfireBlock.LIT)) {
         if (!level.isClientSide()) {
            level.levelEvent(null, 1009, blockpos, 0);
         }

         CampfireBlock.dowse(p_43119_.getPlayer(), level, blockpos, blockstate);
         blockstate2 = blockstate.setValue(CampfireBlock.LIT, false);
      }

      if (blockstate2 != null) {
         if (!level.isClientSide()) {
            level.setBlock(blockpos, blockstate2, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, blockstate2));
            if (player != null) {
               p_43119_.getItemInHand().hurtAndBreak(1, player, p_43119_.getHand().asEquipmentSlot());
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
