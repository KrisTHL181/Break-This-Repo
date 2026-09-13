package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class DyeItem extends Item implements SignApplicator {
   public DyeItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult interactLivingEntity(final ItemStack itemStack, final Player player, final LivingEntity target, final InteractionHand type) {
      if (target instanceof Sheep sheep && sheep.isAlive() && !sheep.isSheared()) {
         DyeColor dyeColor = itemStack.get(DataComponents.DYE);
         if (dyeColor != null && sheep.getColor() != dyeColor) {
            sheep.level().playSound(player, sheep, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.level().isClientSide()) {
               sheep.setColor(dyeColor);
               itemStack.shrink(1);
            }

            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   @Override
   public boolean tryApplyToSign(final Level level, final SignBlockEntity sign, final boolean isFrontText, final ItemStack item, final Player player) {
      DyeColor dye = item.get(DataComponents.DYE);
      if (dye != null && sign.updateText(text -> text.setColor(dye), isFrontText)) {
         level.playSound(null, sign.getBlockPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }
}
