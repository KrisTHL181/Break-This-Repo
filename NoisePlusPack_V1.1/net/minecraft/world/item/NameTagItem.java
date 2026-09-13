package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class NameTagItem extends Item {
   public NameTagItem(Item.Properties p_42952_) {
      super(p_42952_);
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack p_42954_, Player p_42955_, LivingEntity p_42956_, InteractionHand p_42957_) {
      Component component = p_42954_.get(DataComponents.CUSTOM_NAME);
      if (component != null && p_42956_.getType().canSerialize()) {
         if (!p_42955_.level().isClientSide() && p_42956_.isAlive()) {
            p_42956_.setCustomName(component);
            if (p_42956_ instanceof Mob mob) {
               mob.setPersistenceRequired();
            }

            p_42954_.shrink(1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
