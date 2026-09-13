package net.minecraft.world.entity.monster;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public interface CrossbowAttackMob extends RangedAttackMob {
   void setChargingCrossbow(boolean var1);

   @Nullable LivingEntity getTarget();

   void onCrossbowAttackPerformed();

   default void performCrossbowAttack(LivingEntity p_32337_, float p_32338_) {
      InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(p_32337_, Items.CROSSBOW);
      ItemStack itemstack = p_32337_.getItemInHand(interactionhand);
      if (itemstack.getItem() instanceof CrossbowItem crossbowitem) {
         crossbowitem.performShooting(
            p_32337_.level(), p_32337_, interactionhand, itemstack, p_32338_, 14 - p_32337_.level().getDifficulty().getId() * 4, this.getTarget()
         );
      }

      this.onCrossbowAttackPerformed();
   }
}
