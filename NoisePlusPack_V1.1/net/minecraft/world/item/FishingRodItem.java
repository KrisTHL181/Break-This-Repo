package net.minecraft.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class FishingRodItem extends Item {
   public FishingRodItem(Item.Properties p_41285_) {
      super(p_41285_);
   }

   @Override
   public InteractionResult use(Level p_41290_, Player p_41291_, InteractionHand p_41292_) {
      ItemStack itemstack = p_41291_.getItemInHand(p_41292_);
      if (p_41291_.fishing != null) {
         if (!p_41290_.isClientSide()) {
            int i = p_41291_.fishing.retrieve(itemstack);
            itemstack.hurtAndBreak(i, p_41291_, p_41292_.asEquipmentSlot());
         }

         p_41290_.playSound(
            null,
            p_41291_.getX(),
            p_41291_.getY(),
            p_41291_.getZ(),
            SoundEvents.FISHING_BOBBER_RETRIEVE,
            SoundSource.NEUTRAL,
            1.0F,
            0.4F / (p_41290_.getRandom().nextFloat() * 0.4F + 0.8F)
         );
         itemstack.causeUseVibration(p_41291_, GameEvent.ITEM_INTERACT_FINISH);
      } else {
         p_41290_.playSound(
            null,
            p_41291_.getX(),
            p_41291_.getY(),
            p_41291_.getZ(),
            SoundEvents.FISHING_BOBBER_THROW,
            SoundSource.NEUTRAL,
            0.5F,
            0.4F / (p_41290_.getRandom().nextFloat() * 0.4F + 0.8F)
         );
         if (p_41290_ instanceof ServerLevel serverlevel) {
            int j = (int)(EnchantmentHelper.getFishingTimeReduction(serverlevel, itemstack, p_41291_) * 20.0F);
            int k = EnchantmentHelper.getFishingLuckBonus(serverlevel, itemstack, p_41291_);
            Projectile.spawnProjectile(new FishingHook(p_41291_, p_41290_, k, j), serverlevel, itemstack);
         }

         p_41291_.awardStat(Stats.ITEM_USED.get(this));
         itemstack.causeUseVibration(p_41291_, GameEvent.ITEM_INTERACT_START);
      }

      return InteractionResult.SUCCESS;
   }
}
