package net.minecraft.world.entity.ai.goal;

import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class UseItemGoal<T extends Mob> extends Goal {
   private final T mob;
   private final ItemStack item;
   private final Predicate<? super T> canUseSelector;
   private final @Nullable SoundEvent finishUsingSound;

   public UseItemGoal(final T mob, final ItemStack item, final @Nullable SoundEvent finishUsingSound, final Predicate<? super T> canUseSelector) {
      this.mob = mob;
      this.item = item;
      this.finishUsingSound = finishUsingSound;
      this.canUseSelector = canUseSelector;
   }

   @Override
   public boolean canUse() {
      return this.canUseSelector.test(this.mob);
   }

   @Override
   public boolean canContinueToUse() {
      return this.mob.isUsingItem();
   }

   @Override
   public void start() {
      this.mob.setItemSlot(EquipmentSlot.MAINHAND, this.item.copy());
      this.mob.startUsingItem(InteractionHand.MAIN_HAND);
   }

   @Override
   public void stop() {
      this.mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      if (this.finishUsingSound != null) {
         this.mob.playSound(this.finishUsingSound, 1.0F, this.mob.getRandom().nextFloat() * 0.2F + 0.9F);
      }
   }
}
