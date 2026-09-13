package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

public class MerchantContainer implements Container {
   private final Merchant merchant;
   private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
   private @Nullable MerchantOffer activeOffer;
   private int selectionHint;
   private int futureXp;

   public MerchantContainer(final Merchant villager) {
      this.merchant = villager;
   }

   @Override
   public int getContainerSize() {
      return this.itemStacks.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemStack : this.itemStacks) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(final int slot) {
      return this.itemStacks.get(slot);
   }

   @Override
   public ItemStack removeItem(final int slot, final int count) {
      ItemStack itemStack = this.itemStacks.get(slot);
      if (slot == 2 && !itemStack.isEmpty()) {
         return ContainerHelper.removeItem(this.itemStacks, slot, itemStack.getCount());
      }

      ItemStack result = ContainerHelper.removeItem(this.itemStacks, slot, count);
      if (!result.isEmpty() && this.isPaymentSlot(slot)) {
         this.updateSellItem();
      }

      return result;
   }

   private boolean isPaymentSlot(final int slot) {
      return slot == 0 || slot == 1;
   }

   @Override
   public ItemStack removeItemNoUpdate(final int slot) {
      return ContainerHelper.takeItem(this.itemStacks, slot);
   }

   @Override
   public void setItem(final int slot, final ItemStack itemStack) {
      this.itemStacks.set(slot, itemStack);
      itemStack.limitSize(this.getMaxStackSize(itemStack));
      if (this.isPaymentSlot(slot)) {
         this.updateSellItem();
      }
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.merchant.getTradingPlayer() == player;
   }

   @Override
   public void setChanged() {
      this.updateSellItem();
   }

   public void updateSellItem() {
      this.activeOffer = null;
      ItemStack buyA;
      ItemStack buyB;
      if (this.itemStacks.get(0).isEmpty()) {
         buyA = this.itemStacks.get(1);
         buyB = ItemStack.EMPTY;
      } else {
         buyA = this.itemStacks.get(0);
         buyB = this.itemStacks.get(1);
      }

      if (buyA.isEmpty()) {
         this.setItem(2, ItemStack.EMPTY);
         this.futureXp = 0;
      } else {
         MerchantOffers offers = this.merchant.getOffers();
         if (!offers.isEmpty()) {
            MerchantOffer offer = offers.getRecipeFor(buyA, buyB, this.selectionHint);
            if (offer == null || offer.isOutOfStock()) {
               this.activeOffer = offer;
               offer = offers.getRecipeFor(buyB, buyA, this.selectionHint);
            }

            if (offer != null && !offer.isOutOfStock()) {
               this.activeOffer = offer;
               this.setItem(2, offer.assemble());
               this.futureXp = offer.getXp();
            } else {
               this.setItem(2, ItemStack.EMPTY);
               this.futureXp = 0;
            }
         }

         this.merchant.notifyTradeUpdated(this.getItem(2));
      }
   }

   public @Nullable MerchantOffer getActiveOffer() {
      return this.activeOffer;
   }

   public void setSelectionHint(final int selectionHint) {
      this.selectionHint = selectionHint;
      this.updateSellItem();
   }

   @Override
   public void clearContent() {
      this.itemStacks.clear();
   }

   public int getFutureXp() {
      return this.futureXp;
   }
}
