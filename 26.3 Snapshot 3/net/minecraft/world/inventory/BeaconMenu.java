package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.jspecify.annotations.Nullable;

public class BeaconMenu extends AbstractContainerMenu {
   private static final int PAYMENT_SLOT = 0;
   private static final int SLOT_COUNT = 1;
   private static final int DATA_COUNT = 3;
   private static final int INV_SLOT_START = 1;
   private static final int INV_SLOT_END = 28;
   private static final int USE_ROW_SLOT_START = 28;
   private static final int USE_ROW_SLOT_END = 37;
   private static final int NO_EFFECT = 0;
   private final Container beacon = new SimpleContainer(1) {
      @Override
      public boolean canPlaceItem(final int slot, final ItemStack itemStack) {
         return itemStack.is(ItemTags.BEACON_PAYMENT_ITEMS);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }
   };
   private final BeaconMenu.PaymentSlot paymentSlot;
   private final ContainerLevelAccess access;
   private final ContainerData beaconData;

   public BeaconMenu(final int containerId, final Container inventory) {
      this(containerId, inventory, new SimpleContainerData(3), ContainerLevelAccess.NULL);
   }

   public BeaconMenu(final int containerId, final Container inventory, final ContainerData beaconData, final ContainerLevelAccess access) {
      super(MenuType.BEACON, containerId);
      checkContainerDataCount(beaconData, 3);
      this.beaconData = beaconData;
      this.access = access;
      this.paymentSlot = new BeaconMenu.PaymentSlot(this.beacon, 0, 136, 110);
      this.addSlot(this.paymentSlot);
      this.addDataSlots(beaconData);
      this.addStandardInventorySlots(inventory, 36, 137);
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      if (!player.level().isClientSide()) {
         ItemStack itemStack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());
         if (!itemStack.isEmpty()) {
            player.drop(itemStack, false);
         }
      }
   }

   @Override
   public boolean stillValid(final Player player) {
      return stillValid(this.access, player, Blocks.BEACON);
   }

   @Override
   public void setData(final int id, final int value) {
      super.setData(id, value);
      this.broadcastChanges();
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex == 0) {
            if (!this.moveItemStackTo(stack, 1, 37, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, clicked);
         } else if (!this.paymentSlot.hasItem() && this.paymentSlot.mayPlace(stack) && stack.getCount() == 1) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= 1 && slotIndex < 28) {
            if (!this.moveItemStackTo(stack, 28, 37, false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= 28 && slotIndex < 37) {
            if (!this.moveItemStackTo(stack, 1, 28, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 1, 37, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, stack);
      }

      return clicked;
   }

   public int getLevels() {
      return this.beaconData.get(0);
   }

   public static int encodeEffect(final @Nullable Holder<MobEffect> mobEffect) {
      return mobEffect == null ? 0 : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().getId(mobEffect) + 1;
   }

   public static @Nullable Holder<MobEffect> decodeEffect(final int id) {
      return id == 0 ? null : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().byId(id - 1);
   }

   public @Nullable Holder<MobEffect> getPrimaryEffect() {
      return decodeEffect(this.beaconData.get(1));
   }

   public @Nullable Holder<MobEffect> getSecondaryEffect() {
      return decodeEffect(this.beaconData.get(2));
   }

   public boolean updateEffects(final Optional<Holder<MobEffect>> primary, final Optional<Holder<MobEffect>> secondary) {
      if (this.paymentSlot.hasItem()) {
         int levels = this.getLevels();
         Holder<MobEffect> primaryEffect = primary.orElse(null);
         Holder<MobEffect> secondaryEffect = secondary.orElse(null);
         if (!BeaconBlockEntity.validateEffects(primaryEffect, secondaryEffect, levels)) {
            return false;
         }

         this.beaconData.set(1, encodeEffect(primaryEffect));
         this.beaconData.set(2, encodeEffect(secondaryEffect));
         this.paymentSlot.remove(1);
         this.access.execute(Level::blockEntityChanged);
         return true;
      } else {
         return false;
      }
   }

   public boolean hasPayment() {
      return !this.beacon.getItem(0).isEmpty();
   }

   private static class PaymentSlot extends Slot {
      public PaymentSlot(final Container container, final int slot, final int x, final int y) {
         super(container, slot, x, y);
      }

      @Override
      public boolean mayPlace(final ItemStack itemStack) {
         return itemStack.is(ItemTags.BEACON_PAYMENT_ITEMS);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }
   }
}
