package net.minecraft.world.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class FlintAndSteelItem extends Item {
   public FlintAndSteelItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Player player = context.getPlayer();
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!CampfireBlock.canLight(state) && !CandleBlock.canLight(state) && !CandleCakeBlock.canLight(state)) {
         BlockPos relativePos = pos.relative(context.getClickedFace());
         if (BaseFireBlock.canBePlacedAt(level, relativePos, context.getHorizontalDirection())) {
            level.playSound(player, relativePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            BlockState fireState = BaseFireBlock.getState(level, relativePos);
            level.setBlock(relativePos, fireState, 11);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
            ItemStack itemStack = context.getItemInHand();
            if (player instanceof ServerPlayer serverPlayer) {
               CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, relativePos, itemStack);
               itemStack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      } else {
         level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
         level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 11);
         level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
         if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      }
   }
}
