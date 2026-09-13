package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class ShearsItem extends Item {
   public ShearsItem(final Item.Properties properties) {
      super(properties);
   }

   public static Tool createToolProperties() {
      HolderGetter<Block> registrationLookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
      return new Tool(
         List.of(
            Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
            Tool.Rule.overrideSpeed(registrationLookup.getOrThrow(BlockTags.SHEARS_EXTREME_BREAKING_SPEED), 15.0F),
            Tool.Rule.overrideSpeed(registrationLookup.getOrThrow(BlockTags.SHEARS_MAJOR_BREAKING_SPEED), 5.0F),
            Tool.Rule.overrideSpeed(registrationLookup.getOrThrow(BlockTags.SHEARS_MINOR_BREAKING_SPEED), 2.0F)
         ),
         1.0F,
         1,
         true
      );
   }

   @Override
   public boolean mineBlock(final ItemStack itemStack, final Level level, final BlockState state, final BlockPos pos, final LivingEntity miner) {
      Tool tool = itemStack.get(DataComponents.TOOL);
      if (tool == null) {
         return false;
      }

      if (!level.isClientSide() && !state.is(BlockTags.FIRE) && tool.damagePerBlock() > 0) {
         itemStack.hurtAndBreak(tool.damagePerBlock(), miner, EquipmentSlot.MAINHAND);
      }

      return true;
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof GrowingPlantHeadBlock plantBlock && !plantBlock.isMaxAge(state)) {
         Player player = context.getPlayer();
         ItemStack itemInHand = context.getItemInHand();
         if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, itemInHand);
         }

         level.playSound(player, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
         BlockState newState = plantBlock.getMaxAgeState(state);
         level.setBlockAndUpdate(pos, newState);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(context.getPlayer(), newState));
         if (player != null) {
            itemInHand.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useOn(context);
      }
   }
}
