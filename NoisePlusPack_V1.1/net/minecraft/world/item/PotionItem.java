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
   public PotionItem(Item.Properties p_42979_) {
      super(p_42979_);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack itemstack = super.getDefaultInstance();
      itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
      return itemstack;
   }

   @Override
   public InteractionResult useOn(UseOnContext p_220235_) {
      Level level = p_220235_.getLevel();
      BlockPos blockpos = p_220235_.getClickedPos();
      Player player = p_220235_.getPlayer();
      ItemStack itemstack = p_220235_.getItemInHand();
      PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      BlockState blockstate = level.getBlockState(blockpos);
      if (p_220235_.getClickedFace() != Direction.DOWN && blockstate.is(BlockTags.CONVERTABLE_TO_MUD) && potioncontents.is(Potions.WATER)) {
         level.playSound(null, blockpos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
         player.setItemInHand(p_220235_.getHand(), ItemUtils.createFilledResult(itemstack, player, new ItemStack(Items.GLASS_BOTTLE)));
         if (!level.isClientSide()) {
            ServerLevel serverlevel = (ServerLevel)level;

            for (int i = 0; i < 5; i++) {
               serverlevel.sendParticles(
                  ParticleTypes.SPLASH,
                  blockpos.getX() + level.random.nextDouble(),
                  blockpos.getY() + 1,
                  blockpos.getZ() + level.random.nextDouble(),
                  1,
                  0.0,
                  0.0,
                  0.0,
                  1.0
               );
            }
         }

         level.playSound(null, blockpos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.gameEvent(null, GameEvent.FLUID_PLACE, blockpos);
         level.setBlockAndUpdate(blockpos, Blocks.MUD.defaultBlockState());
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public Component getName(ItemStack p_367430_) {
      PotionContents potioncontents = p_367430_.get(DataComponents.POTION_CONTENTS);
      return potioncontents != null ? potioncontents.getName(this.descriptionId + ".effect.") : super.getName(p_367430_);
   }
}
