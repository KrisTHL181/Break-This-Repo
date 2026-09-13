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

   public MerchantContainer(Merchant p_40003_) {
      this.merchant = p_40003_;
   }

   @Override
   public int getContainerSize() {
      return this.itemStacks.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemstack : this.itemStacks) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int p_40008_) {
      return this.itemStacks.get(p_40008_);
   }

   @Override
   public ItemStack removeItem(int p_40010_, int p_40011_) {
      ItemStack itemstack = this.itemStacks.get(p_40010_);
      if (p_40010_ == 2 && !itemstack.isEmpty()) {
         return ContainerHelper.removeItem(this.itemStacks, p_40010_, itemstack.getCount());
      }

      ItemStack itemstack1 = ContainerHelper.removeItem(this.itemStacks, p_40010_, p_40011_);
      if (!itemstack1.isEmpty() && this.isPaymentSlot(p_40010_)) {
         this.updateSellItem();
      }

      return itemstack1;
   }

   private boolean isPaymentSlot(int p_40023_) {
      return p_40023_ == 0 || p_40023_ == 1;
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_40018_) {
      return ContainerHelper.takeItem(this.itemStacks, p_40018_);
   }

   @Override
   public void setItem(int p_40013_, ItemStack p_40014_) {
      this.itemStacks.set(p_40013_, p_40014_);
      p_40014_.limitSize(this.getMaxStackSize(p_40014_));
      if (this.isPaymentSlot(p_40013_)) {
         this.updateSellItem();
      }
   }

   @Override
   public boolean stillValid(Player p_40016_) {
      return this.merchant.getTradingPlayer() == p_40016_;
   }

   @Override
   public void setChanged() {
      this.updateSellItem();
   }

   public void updateSellItem() {
      this.activeOffer = null;
      ItemStack itemstack;
      ItemStack itemstack1;
      if (this.itemStacks.get(0).isEmpty()) {
         itemstack = this.itemStacks.get(1);
         itemstack1 = ItemStack.EMPTY;
      } else {
         itemstack = this.itemStacks.get(0);
         itemstack1 = this.itemStacks.get(1);
      }

      if (itemstack.isEmpty()) {
         this.setItem(2, ItemStack.EMPTY);
         this.futureXp = 0;
      } else {
         MerchantOffers merchantoffers = this.merchant.getOffers();
         if (!merchantoffers.isEmpty()) {
            MerchantOffer merchantoffer = merchantoffers.getRecipeFor(itemstack, itemstack1, this.selectionHint);
            if (merchantoffer == null || merchantoffer.isOutOfStock()) {
               this.activeOffer = merchantoffer;
               merchantoffer = merchantoffers.getRecipeFor(itemstack1, itemstack, this.selectionHint);
            }

            if (merchantoffer != null && !merchantoffer.isOutOfStock()) {
               this.activeOffer = merchantoffer;
               this.setItem(2, merchantoffer.assemble());
               this.futureXp = merchantoffer.getXp();
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

   public void setSelectionHint(int p_40021_) {
      this.selectionHint = p_40021_;
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
