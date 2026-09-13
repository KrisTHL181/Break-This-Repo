package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SpyglassItem extends Item {
   public static final int USE_DURATION = 1200;
   public static final float ZOOM_FOV_MODIFIER = 0.1F;

   public SpyglassItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      return 1200;
   }

   @Override
   public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
      return ItemUseAnimation.SPYGLASS;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
      player.awardStat(Stats.ITEM_USED.get(this));
      return ItemUtils.startUsingInstantly(level, player, hand);
   }

   @Override
   public ItemStack finishUsingItem(final ItemStack itemStack, final Level level, final LivingEntity entity) {
      this.stopUsing(entity);
      return itemStack;
   }

   @Override
   public boolean releaseUsing(final ItemStack itemStack, final Level level, final LivingEntity entity, final int remainingTime) {
      this.stopUsing(entity);
      return true;
   }

   private void stopUsing(final LivingEntity entity) {
      entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
   }
}
