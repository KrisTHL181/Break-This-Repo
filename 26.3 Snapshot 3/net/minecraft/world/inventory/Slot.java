package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class Slot {
   private final int slot;
   public final Container container;
   public int index;
   public final int x;
   public final int y;

   public Slot(final Container container, final int slot, final int x, final int y) {
      this.container = container;
      this.slot = slot;
      this.x = x;
      this.y = y;
   }

   public void onQuickCraft(final ItemStack picked, final ItemStack original) {
      int count = original.getCount() - picked.getCount();
      if (count > 0) {
         this.onQuickCraft(original, count);
      }
   }

   protected void onQuickCraft(final ItemStack picked, final int count) {
   }

   protected void onSwapCraft(final int count) {
   }

   protected void checkTakeAchievements(final ItemStack carried) {
   }

   public void onTake(final Player player, final ItemStack carried) {
      this.setChanged();
   }

   public boolean mayPlace(final ItemStack itemStack) {
      return true;
   }

   public ItemStack getItem() {
      return this.container.getItem(this.slot);
   }

   public boolean hasItem() {
      return !this.getItem().isEmpty();
   }

   public void setByPlayer(final ItemStack itemStack) {
      this.setByPlayer(itemStack, this.getItem());
   }

   public void setByPlayer(final ItemStack itemStack, final ItemStack previous) {
      this.set(itemStack);
   }

   public void set(final ItemStack itemStack) {
      this.container.setItem(this.slot, itemStack);
      this.setChanged();
   }

   public void setChanged() {
      this.container.setChanged();
   }

   public int getMaxStackSize() {
      return this.container.getMaxStackSize();
   }

   public int getMaxStackSize(final ItemStack itemStack) {
      return Math.min(this.getMaxStackSize(), itemStack.getMaxStackSize());
   }

   public @Nullable Identifier getNoItemIcon() {
      return null;
   }

   public ItemStack remove(final int amount) {
      return this.container.removeItem(this.slot, amount);
   }

   public boolean mayPickup(final Player player) {
      return true;
   }

   public boolean isActive() {
      return true;
   }

   public Optional<ItemStack> tryRemove(int amount, final int maxAmount, final Player player) {
      if (!this.mayPickup(player)) {
         return Optional.empty();
      }

      if (!this.allowModification(player) && maxAmount < this.getItem().getCount()) {
         return Optional.empty();
      }

      amount = Math.min(amount, maxAmount);
      ItemStack result = this.remove(amount);
      if (result.isEmpty()) {
         return Optional.empty();
      }

      if (this.getItem().isEmpty()) {
         this.setByPlayer(ItemStack.EMPTY, result);
      }

      return Optional.of(result);
   }

   public ItemStack safeTake(final int amount, final int maxAmount, final Player player) {
      Optional<ItemStack> result = this.tryRemove(amount, maxAmount, player);
      result.ifPresent(item -> this.onTake(player, item));
      return result.orElse(ItemStack.EMPTY);
   }

   public ItemStack safeClone(final Player player) {
      ItemStack item = this.getItem();
      return item.copyWithCount(item.getMaxStackSize());
   }

   public ItemStack safeInsert(final ItemStack stack) {
      return this.safeInsert(stack, stack.getCount());
   }

   public ItemStack safeInsert(final ItemStack inputStack, final int inputAmount) {
      if (!inputStack.isEmpty() && this.mayPlace(inputStack)) {
         ItemStack slotStack = this.getItem();
         int transferableItemCount = Math.min(Math.min(inputAmount, inputStack.getCount()), this.getMaxStackSize(inputStack) - slotStack.getCount());
         if (transferableItemCount <= 0) {
            return inputStack;
         }

         if (slotStack.isEmpty()) {
            this.setByPlayer(inputStack.split(transferableItemCount));
         } else if (ItemStack.isSameItemSameComponents(slotStack, inputStack)) {
            inputStack.shrink(transferableItemCount);
            slotStack.grow(transferableItemCount);
            this.setByPlayer(slotStack);
         }

         return inputStack;
      } else {
         return inputStack;
      }
   }

   public boolean allowModification(final Player player) {
      return this.mayPickup(player) && this.mayPlace(this.getItem());
   }

   public int getContainerSlot() {
      return this.slot;
   }

   public boolean isHighlightable() {
      return true;
   }

   public boolean isFake() {
      return false;
   }
}
