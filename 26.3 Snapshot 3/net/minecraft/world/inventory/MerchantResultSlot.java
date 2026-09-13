package net.minecraft.world.inventory;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class MerchantResultSlot extends Slot {
   private final MerchantContainer slots;
   private final Player player;
   private int removeCount;
   private final Merchant merchant;

   public MerchantResultSlot(final Player player, final Merchant merchant, final MerchantContainer slots, final int id, final int x, final int y) {
      super(slots, id, x, y);
      this.player = player;
      this.merchant = merchant;
      this.slots = slots;
   }

   @Override
   public boolean mayPlace(final ItemStack itemStack) {
      return false;
   }

   @Override
   public ItemStack remove(final int amount) {
      if (this.hasItem()) {
         this.removeCount = this.removeCount + Math.min(amount, this.getItem().getCount());
      }

      return super.remove(amount);
   }

   @Override
   protected void onQuickCraft(final ItemStack picked, final int count) {
      this.removeCount += count;
      this.checkTakeAchievements(picked);
   }

   @Override
   protected void checkTakeAchievements(final ItemStack carried) {
      carried.onCraftedBy(this.player, this.removeCount);
      this.removeCount = 0;
   }

   @Override
   public void onTake(final Player player, final ItemStack carried) {
      this.checkTakeAchievements(carried);
      MerchantOffer offer = this.slots.getActiveOffer();
      if (offer != null) {
         ItemStack buyA = this.slots.getItem(0);
         ItemStack buyB = this.slots.getItem(1);
         if (offer.take(buyA, buyB) || offer.take(buyB, buyA)) {
            this.merchant.notifyTrade(offer);
            player.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.slots.setItem(0, buyA);
            this.slots.setItem(1, buyB);
         }

         this.merchant.overrideXp(this.merchant.getVillagerXp() + offer.getXp());
      }
   }
}
