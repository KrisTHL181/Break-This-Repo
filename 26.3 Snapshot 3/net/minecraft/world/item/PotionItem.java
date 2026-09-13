package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item {
   public PotionItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack itemStack = super.getDefaultInstance();
      itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
      return itemStack;
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Player player = context.getPlayer();
      ItemStack itemStack = context.getItemInHand();
      PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      BlockState blockState = level.getBlockState(pos);
      if (context.getClickedFace() != Direction.DOWN && blockState.is(BlockTags.CONVERTIBLE_TO_MUD) && potionContents.is(Potions.WATER)) {
         level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
         player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
         if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel)level;

            for (int i = 0; i < 5; i++) {
               serverLevel.sendParticles(
                  ParticleTypes.SPLASH,
                  pos.getX() + level.getRandom().nextDouble(),
                  pos.getY() + 1,
                  pos.getZ() + level.getRandom().nextDouble(),
                  1,
                  0.0,
                  0.0,
                  0.0,
                  1.0
               );
            }
         }

         level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
         level.setBlockAndUpdate(pos, Blocks.MUD.defaultBlockState());
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public Component getName(final ItemStack itemStack) {
      PotionContents potion = itemStack.get(DataComponents.POTION_CONTENTS);
      return potion != null ? potion.getName(this.descriptionId + ".effect.") : super.getName(itemStack);
   }
}
