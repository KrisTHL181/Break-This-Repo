package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class NameTagItem extends Item {
   public NameTagItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult interactLivingEntity(final ItemStack itemStack, final Player player, final LivingEntity target, final InteractionHand type) {
      Component customName = itemStack.get(DataComponents.CUSTOM_NAME);
      if (customName != null && target.getType().canSerialize()) {
         if (!player.level().isClientSide() && target.isAlive()) {
            target.setCustomName(customName);
            if (target instanceof Mob mob) {
               mob.setPersistenceRequired();
            }

            itemStack.shrink(1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
