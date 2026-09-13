package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int SLOT_CLICKED_OUTSIDE = -999;
   public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
   public static final int QUICKCRAFT_TYPE_GREEDY = 1;
   public static final int QUICKCRAFT_TYPE_CLONE = 2;
   public static final int QUICKCRAFT_HEADER_START = 0;
   public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
   public static final int QUICKCRAFT_HEADER_END = 2;
   public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
   public static final int SLOTS_PER_ROW = 9;
   public static final int SLOT_SIZE = 18;
   private final NonNullList<ItemStack> lastSlots = NonNullList.create();
   public final NonNullList<Slot> slots = NonNullList.create();
   private final List<DataSlot> dataSlots = Lists.newArrayList();
   private ItemStack carried = ItemStack.EMPTY;
   private final NonNullList<RemoteSlot> remoteSlots = NonNullList.create();
   private final IntList remoteDataSlots = new IntArrayList();
   private RemoteSlot remoteCarried = RemoteSlot.PLACEHOLDER;
   private int stateId;
   private final @Nullable MenuType<?> menuType;
   public final int containerId;
   private int quickcraftType = -1;
   private int quickcraftStatus;
   private final Set<Slot> quickcraftSlots = Sets.newHashSet();
   private final List<ContainerListener> containerListeners = Lists.newArrayList();
   private @Nullable ContainerSynchronizer synchronizer;
   private boolean suppressRemoteUpdates;

   protected AbstractContainerMenu(final @Nullable MenuType<?> menuType, final int containerId) {
      this.menuType = menuType;
      this.containerId = containerId;
   }

   protected void addInventoryHotbarSlots(final Container inventory, final int left, final int top) {
      for (int x = 0; x < 9; x++) {
         this.addSlot(new Slot(inventory, x, left + x * 18, top));
      }
   }

   protected void addInventoryExtendedSlots(final Container inventory, final int left, final int top) {
      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(inventory, x + (y + 1) * 9, left + x * 18, top + y * 18));
         }
      }
   }

   protected void addStandardInventorySlots(final Container container, final int left, final int top) {
      this.addInventoryExtendedSlots(container, left, top);
      int hotbarSeparator = 4;
      int topToHotbar = 58;
      this.addInventoryHotbarSlots(container, left, top + 58);
   }

   protected static boolean stillValid(final ContainerLevelAccess access, final Player player, final Block block) {
      return access.evaluate((level, pos) -> !level.getBlockState(pos).is(block) ? false : player.isWithinBlockInteractionRange(pos, 4.0), true);
   }

   public MenuType<?> getType() {
      if (this.menuType == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.menuType;
      }
   }

   protected static void checkContainerSize(final Container container, final int expected) {
      int actual = container.getContainerSize();
      if (actual < expected) {
         throw new IllegalArgumentException("Container size " + actual + " is smaller than expected " + expected);
      }
   }

   protected static void checkContainerDataCount(final ContainerData data, final int expected) {
      int actual = data.getCount();
      if (actual < expected) {
         throw new IllegalArgumentException("Container data count " + actual + " is smaller than expected " + expected);
      }
   }

   public boolean isValidSlotIndex(final int slotIndex) {
      return slotIndex == -1 || slotIndex == -999 || slotIndex < this.slots.size();
   }

   protected Slot addSlot(final Slot slot) {
      slot.index = this.slots.size();
      this.slots.add(slot);
      this.lastSlots.add(ItemStack.EMPTY);
      this.remoteSlots.add(this.synchronizer != null ? this.synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
      return slot;
   }

   protected DataSlot addDataSlot(final DataSlot dataSlot) {
      this.dataSlots.add(dataSlot);
      this.remoteDataSlots.add(0);
      return dataSlot;
   }

   protected void addDataSlots(final ContainerData container) {
      for (int i = 0; i < container.getCount(); i++) {
         this.addDataSlot(DataSlot.forContainer(container, i));
      }
   }

   public void addSlotListener(final ContainerListener listener) {
      if (!this.containerListeners.contains(listener)) {
         this.containerListeners.add(listener);
         this.broadcastChanges();
      }
   }

   public void setSynchronizer(final ContainerSynchronizer synchronizer) {
      this.synchronizer = synchronizer;
      this.remoteCarried = synchronizer.createSlot();
      this.remoteSlots.replaceAll(ignored -> synchronizer.createSlot());
      this.sendAllDataToRemote();
   }

   public void sendAllDataToRemote() {
      List<ItemStack> itemsToSend = new ArrayList<>(this.slots.size());
      int i = 0;

      for (int slotsSize = this.slots.size(); i < slotsSize; i++) {
         ItemStack slotContents = this.slots.get(i).getItem();
         itemsToSend.add(slotContents.copy());
         this.remoteSlots.get(i).force(slotContents);
      }

      ItemStack carried = this.getCarried();
      this.remoteCarried.force(carried);
      int ix = 0;

      for (int slotsSize = this.dataSlots.size(); ix < slotsSize; ix++) {
         this.remoteDataSlots.set(ix, this.dataSlots.get(ix).get());
      }

      if (this.synchronizer != null) {
         this.synchronizer.sendInitialData(this, itemsToSend, carried.copy(), this.remoteDataSlots.toIntArray());
      }
   }

   public void removeSlotListener(final ContainerListener listener) {
      this.containerListeners.remove(listener);
   }

   public NonNullList<ItemStack> getItems() {
      NonNullList<ItemStack> itemStacks = NonNullList.create();

      for (Slot slot : this.slots) {
         itemStacks.add(slot.getItem());
      }

      return itemStacks;
   }

   public void broadcastChanges() {
      for (int i = 0; i < this.slots.size(); i++) {
         ItemStack current = this.slots.get(i).getItem();
         Supplier<ItemStack> currentCopy = Suppliers.memoize(current::copy);
         this.triggerSlotListeners(i, current, currentCopy);
         this.synchronizeSlotToRemote(i, current, currentCopy);
      }

      this.synchronizeCarriedToRemote();

      for (int i = 0; i < this.dataSlots.size(); i++) {
         DataSlot current = this.dataSlots.get(i);
         int currentValue = current.get();
         if (current.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(i, currentValue);
         }

         this.synchronizeDataSlotToRemote(i, currentValue);
      }
   }

   public void broadcastFullState() {
      for (int i = 0; i < this.slots.size(); i++) {
         ItemStack current = this.slots.get(i).getItem();
         this.triggerSlotListeners(i, current, current::copy);
      }

      for (int i = 0; i < this.dataSlots.size(); i++) {
         DataSlot current = this.dataSlots.get(i);
         if (current.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(i, current.get());
         }
      }

      this.sendAllDataToRemote();
   }

   private void updateDataSlotListeners(final int id, final int currentValue) {
      for (ContainerListener containerListener : this.containerListeners) {
         containerListener.dataChanged(this, id, currentValue);
      }
   }

   private void triggerSlotListeners(final int i, final ItemStack current, final Supplier<ItemStack> currentCopy) {
      ItemStack localExpected = this.lastSlots.get(i);
      if (!ItemStack.matches(localExpected, current)) {
         ItemStack newItem = currentCopy.get();
         this.lastSlots.set(i, newItem);

         for (ContainerListener containerListener : this.containerListeners) {
            containerListener.slotChanged(this, i, newItem);
         }
      }
   }

   private void synchronizeSlotToRemote(final int i, final ItemStack current, final Supplier<ItemStack> currentCopy) {
      if (!this.suppressRemoteUpdates) {
         RemoteSlot remoteExpected = this.remoteSlots.get(i);
         if (!remoteExpected.matches(current)) {
            remoteExpected.force(current);
            if (this.synchronizer != null) {
               this.synchronizer.sendSlotChange(this, i, currentCopy.get());
            }
         }
      }
   }

   private void synchronizeDataSlotToRemote(final int i, final int current) {
      if (!this.suppressRemoteUpdates) {
         int remoteExpected = this.remoteDataSlots.getInt(i);
         if (remoteExpected != current) {
            this.remoteDataSlots.set(i, current);
            if (this.synchronizer != null) {
               this.synchronizer.sendDataChange(this, i, current);
            }
         }
      }
   }

   private void synchronizeCarriedToRemote() {
      if (!this.suppressRemoteUpdates) {
         ItemStack carriedItem = this.getCarried();
         if (!this.remoteCarried.matches(carriedItem)) {
            this.remoteCarried.force(carriedItem);
            if (this.synchronizer != null) {
               this.synchronizer.sendCarriedChange(this, carriedItem.copy());
            }
         }
      }
   }

   public void setRemoteSlot(final int slot, final ItemStack itemStack) {
      this.remoteSlots.get(slot).force(itemStack);
   }

   public void setRemoteSlotUnsafe(final int slot, final HashedStack itemStack) {
      if (slot >= 0 && slot < this.remoteSlots.size()) {
         this.remoteSlots.get(slot).receive(itemStack);
      } else {
         LOGGER.debug("Incorrect slot index: {} available slots: {}", slot, this.remoteSlots.size());
      }
   }

   public void setRemoteCarried(final HashedStack carriedItem) {
      this.remoteCarried.receive(carriedItem);
   }

   public boolean clickMenuButton(final Player player, final int buttonId) {
      return false;
   }

   public Slot getSlot(final int index) {
      return this.slots.get(index);
   }

   public abstract ItemStack quickMoveStack(final Player player, final int slotIndex);

   public void setSelectedBundleItemIndex(final int slotIndex, final int selectedItemIndex) {
      if (slotIndex >= 0 && slotIndex < this.slots.size()) {
         ItemStack itemStack = this.slots.get(slotIndex).getItem();
         BundleItem.toggleSelectedItem(itemStack, selectedItemIndex);
      }
   }

   public void clicked(final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
      try {
         this.doClick(slotIndex, buttonNum, containerInput, player);
      } catch (Exception e) {
         CrashReport report = CrashReport.forThrowable(e, "Container click");
         CrashReportCategory category = report.addCategory("Click info");
         category.setDetail("Menu Type", () -> this.menuType != null ? BuiltInRegistries.MENU.getKey(this.menuType).toString() : "<no type>");
         category.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
         category.setDetail("Slot Count", this.slots.size());
         category.setDetail("Slot", slotIndex);
         category.setDetail("Button", buttonNum);
         category.setDetail("Type", containerInput);
         throw new ReportedException(report);
      }
   }

   private void doClick(final int slotIndex, final int buttonNum, final ContainerInput containerInput, final Player player) {
      Inventory inventory = player.getInventory();
      if (containerInput == ContainerInput.QUICK_CRAFT) {
         int expectedStatus = this.quickcraftStatus;
         this.quickcraftStatus = getQuickcraftHeader(buttonNum);
         if ((expectedStatus != 1 || this.quickcraftStatus != 2) && expectedStatus != this.quickcraftStatus) {
            this.resetQuickCraft();
         } else if (this.getCarried().isEmpty()) {
            this.resetQuickCraft();
         } else if (this.quickcraftStatus == 0) {
            this.quickcraftType = getQuickcraftType(buttonNum);
            if (isValidQuickcraftType(this.quickcraftType, player)) {
               this.quickcraftStatus = 1;
               this.quickcraftSlots.clear();
            } else {
               this.resetQuickCraft();
            }
         } else if (this.quickcraftStatus == 1) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack carriedItemStack = this.getCarried();
            if (canItemQuickReplace(slot, carriedItemStack, true)
               && slot.mayPlace(carriedItemStack)
               && (this.quickcraftType == 2 || carriedItemStack.getCount() > this.quickcraftSlots.size())
               && this.canDragTo(slot)) {
               this.quickcraftSlots.add(slot);
            }
         } else if (this.quickcraftStatus == 2) {
            if (!this.quickcraftSlots.isEmpty()) {
               if (this.quickcraftSlots.size() == 1) {
                  int slot = this.quickcraftSlots.iterator().next().index;
                  this.resetQuickCraft();
                  this.doClick(slot, this.quickcraftType, ContainerInput.PICKUP, player);
                  return;
               }

               ItemStack source = this.getCarried().copy();
               if (source.isEmpty()) {
                  this.resetQuickCraft();
                  return;
               }

               int remaining = this.getCarried().getCount();

               for (Slot slot : this.quickcraftSlots) {
                  ItemStack carriedItemStack = this.getCarried();
                  if (slot != null
                     && canItemQuickReplace(slot, carriedItemStack, true)
                     && slot.mayPlace(carriedItemStack)
                     && (this.quickcraftType == 2 || carriedItemStack.getCount() >= this.quickcraftSlots.size())
                     && this.canDragTo(slot)) {
                     int carry = slot.hasItem() ? slot.getItem().getCount() : 0;
                     int maxSize = Math.min(source.getMaxStackSize(), slot.getMaxStackSize(source));
                     int newCount = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots.size(), this.quickcraftType, source) + carry, maxSize);
                     remaining -= newCount - carry;
                     slot.setByPlayer(source.copyWithCount(newCount));
                  }
               }

               source.setCount(remaining);
               this.setCarried(source);
            }

            this.resetQuickCraft();
         } else {
            this.resetQuickCraft();
         }
      } else if (this.quickcraftStatus != 0) {
         this.resetQuickCraft();
      } else if ((containerInput == ContainerInput.PICKUP || containerInput == ContainerInput.QUICK_MOVE) && (buttonNum == 0 || buttonNum == 1)) {
         ClickAction clickAction = buttonNum == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
         if (slotIndex == -999) {
            if (!this.getCarried().isEmpty()) {
               if (clickAction == ClickAction.PRIMARY) {
                  player.drop(this.getCarried(), true);
                  this.setCarried(ItemStack.EMPTY);
               } else {
                  player.drop(this.getCarried().split(1), true);
               }
            }
         } else if (containerInput == ContainerInput.QUICK_MOVE) {
            if (slotIndex < 0) {
               return;
            }

            Slot slot = this.slots.get(slotIndex);
            if (!slot.mayPickup(player)) {
               return;
            }

            ItemStack clicked = this.quickMoveStack(player, slotIndex);

            while (!clicked.isEmpty() && ItemStack.isSameItem(slot.getItem(), clicked)) {
               clicked = this.quickMoveStack(player, slotIndex);
            }
         } else {
            if (slotIndex < 0) {
               return;
            }

            Slot slot = this.slots.get(slotIndex);
            ItemStack clicked = slot.getItem();
            ItemStack carried = this.getCarried();
            player.updateTutorialInventoryAction(carried, slot.getItem(), clickAction);
            if (!this.tryItemClickBehaviourOverride(player, clickAction, slot, clicked, carried)) {
               if (clicked.isEmpty()) {
                  if (!carried.isEmpty()) {
                     int amount = clickAction == ClickAction.PRIMARY ? carried.getCount() : 1;
                     this.setCarried(slot.safeInsert(carried, amount));
                  }
               } else if (slot.mayPickup(player)) {
                  if (carried.isEmpty()) {
                     int amount = clickAction == ClickAction.PRIMARY ? clicked.getCount() : (clicked.getCount() + 1) / 2;
                     Optional<ItemStack> newCarried = slot.tryRemove(amount, Integer.MAX_VALUE, player);
                     newCarried.ifPresent(itemsTaken -> {
                        this.setCarried(itemsTaken);
                        slot.onTake(player, itemsTaken);
                     });
                  } else if (slot.mayPlace(carried)) {
                     if (ItemStack.isSameItemSameComponents(clicked, carried)) {
                        int amount = clickAction == ClickAction.PRIMARY ? carried.getCount() : 1;
                        this.setCarried(slot.safeInsert(carried, amount));
                     } else if (carried.getCount() <= slot.getMaxStackSize(carried)) {
                        this.setCarried(clicked);
                        slot.setByPlayer(carried);
                     }
                  } else if (ItemStack.isSameItemSameComponents(clicked, carried)) {
                     Optional<ItemStack> newCarried = slot.tryRemove(clicked.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                     newCarried.ifPresent(itemsTaken -> {
                        carried.grow(itemsTaken.getCount());
                        slot.onTake(player, itemsTaken);
                     });
                  }
               }
            }

            slot.setChanged();
         }
      } else if (containerInput == ContainerInput.SWAP && (buttonNum >= 0 && buttonNum < 9 || buttonNum == 40)) {
         ItemStack source = inventory.getItem(buttonNum);
         Slot target = this.slots.get(slotIndex);
         ItemStack targetItemStack = target.getItem();
         if (!source.isEmpty() || !targetItemStack.isEmpty()) {
            if (source.isEmpty()) {
               if (target.mayPickup(player)) {
                  inventory.setItem(buttonNum, targetItemStack);
                  target.onSwapCraft(targetItemStack.getCount());
                  target.setByPlayer(ItemStack.EMPTY);
                  target.onTake(player, targetItemStack);
               }
            } else if (targetItemStack.isEmpty()) {
               if (target.mayPlace(source)) {
                  int maxStackSize = target.getMaxStackSize(source);
                  if (source.getCount() > maxStackSize) {
                     target.setByPlayer(source.split(maxStackSize));
                  } else {
                     inventory.setItem(buttonNum, ItemStack.EMPTY);
                     target.setByPlayer(source);
                  }
               }
            } else if (target.mayPickup(player) && target.mayPlace(source)) {
               int maxStackSize = target.getMaxStackSize(source);
               if (source.getCount() > maxStackSize) {
                  target.setByPlayer(source.split(maxStackSize));
                  target.onTake(player, targetItemStack);
                  if (!inventory.add(targetItemStack)) {
                     player.drop(targetItemStack, true);
                  }
               } else {
                  inventory.setItem(buttonNum, targetItemStack);
                  target.setByPlayer(source);
                  target.onTake(player, targetItemStack);
               }
            }
         }
      } else if (containerInput == ContainerInput.CLONE && player.hasInfiniteMaterials() && this.getCarried().isEmpty() && slotIndex >= 0) {
         Slot slot = this.slots.get(slotIndex);
         if (slot.hasItem()) {
            this.setCarried(slot.safeClone(player));
         }
      } else if (containerInput == ContainerInput.THROW && this.getCarried().isEmpty() && slotIndex >= 0) {
         Slot slot = this.slots.get(slotIndex);
         int amount = buttonNum == 0 ? 1 : slot.getItem().getCount();
         if (!player.canDropItems()) {
            return;
         }

         ItemStack itemStack = slot.safeTake(amount, Integer.MAX_VALUE, player);
         player.drop(itemStack, true);
         player.handleCreativeModeItemDrop(itemStack);
         if (buttonNum == 1) {
            while (!itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack)) {
               if (!player.canDropItems()) {
                  return;
               }

               itemStack = slot.safeTake(amount, Integer.MAX_VALUE, player);
               player.drop(itemStack, true);
               player.handleCreativeModeItemDrop(itemStack);
            }
         }
      } else if (containerInput == ContainerInput.PICKUP_ALL && slotIndex >= 0) {
         Slot slot = this.slots.get(slotIndex);
         ItemStack carried = this.getCarried();
         if (!carried.isEmpty() && (!slot.hasItem() || !slot.mayPickup(player))) {
            int start = buttonNum == 0 ? 0 : this.slots.size() - 1;
            int step = buttonNum == 0 ? 1 : -1;

            for (int pass = 0; pass < 2; pass++) {
               for (int i = start; i >= 0 && i < this.slots.size() && carried.getCount() < carried.getMaxStackSize(); i += step) {
                  Slot target = this.slots.get(i);
                  if (target.hasItem() && canItemQuickReplace(target, carried, true) && target.mayPickup(player) && this.canTakeItemForPickAll(carried, target)
                     )
                   {
                     ItemStack itemStack = target.getItem();
                     if (pass != 0 || itemStack.getCount() != itemStack.getMaxStackSize()) {
                        ItemStack removed = target.safeTake(itemStack.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                        carried.grow(removed.getCount());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryItemClickBehaviourOverride(
      final Player player, final ClickAction clickAction, final Slot slot, final ItemStack clicked, final ItemStack carried
   ) {
      FeatureFlagSet enabledFeatures = player.level().enabledFeatures();
      return carried.isItemEnabled(enabledFeatures) && carried.overrideStackedOnOther(slot, clickAction, player)
         ? true
         : clicked.isItemEnabled(enabledFeatures) && clicked.overrideOtherStackedOnMe(carried, slot, clickAction, player, this.createCarriedSlotAccess());
   }

   private SlotAccess createCarriedSlotAccess() {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractContainerMenu.this.getCarried();
         }

         @Override
         public boolean set(final ItemStack itemStack) {
            AbstractContainerMenu.this.setCarried(itemStack);
            return true;
         }
      };
   }

   public boolean canTakeItemForPickAll(final ItemStack carried, final Slot target) {
      return true;
   }

   public void removed(final Player player) {
      if (player instanceof ServerPlayer) {
         ItemStack carried = this.getCarried();
         if (!carried.isEmpty()) {
            dropOrPlaceInInventory(player, carried);
            this.setCarried(ItemStack.EMPTY);
         }
      }
   }

   private static void dropOrPlaceInInventory(final Player player, final ItemStack carried) {
      boolean playerRemovedNotChangingDimension = player.isRemoved() && player.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION;
      boolean serverPlayerHasDisconnected = player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected();
      if (playerRemovedNotChangingDimension || serverPlayerHasDisconnected) {
         player.drop(carried, false);
      } else if (player instanceof ServerPlayer) {
         player.getInventory().placeItemBackInInventory(carried);
      }
   }

   protected void clearContainer(final Player player, final Container container) {
      for (int i = 0; i < container.getContainerSize(); i++) {
         dropOrPlaceInInventory(player, container.removeItemNoUpdate(i));
      }
   }

   public void slotsChanged(final Container container) {
      this.broadcastChanges();
   }

   public void setItem(final int slot, final int stateId, final ItemStack itemStack) {
      this.getSlot(slot).set(itemStack);
      this.stateId = stateId;
   }

   public void initializeContents(final int stateId, final List<ItemStack> items, final ItemStack carried) {
      for (int i = 0; i < items.size(); i++) {
         this.getSlot(i).set(items.get(i));
      }

      this.carried = carried;
      this.stateId = stateId;
   }

   public void setData(final int id, final int value) {
      this.dataSlots.get(id).set(value);
   }

   public abstract boolean stillValid(Player player);

   protected boolean moveItemStackTo(final ItemStack itemStack, final int startSlot, final int endSlot, final boolean backwards) {
      boolean anythingChanged = false;
      int destSlot = startSlot;
      if (backwards) {
         destSlot = endSlot - 1;
      }

      if (itemStack.isStackable()) {
         while (!itemStack.isEmpty() && (backwards ? destSlot >= startSlot : destSlot < endSlot)) {
            Slot slot = this.slots.get(destSlot);
            ItemStack target = slot.getItem();
            if (!target.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, target)) {
               int totalStack = target.getCount() + itemStack.getCount();
               int maxStackSize = slot.getMaxStackSize(target);
               if (totalStack <= maxStackSize) {
                  itemStack.setCount(0);
                  target.setCount(totalStack);
                  slot.setChanged();
                  anythingChanged = true;
               } else if (target.getCount() < maxStackSize) {
                  itemStack.shrink(maxStackSize - target.getCount());
                  target.setCount(maxStackSize);
                  slot.setChanged();
                  anythingChanged = true;
               }
            }

            if (backwards) {
               destSlot--;
            } else {
               destSlot++;
            }
         }
      }

      if (!itemStack.isEmpty()) {
         if (backwards) {
            destSlot = endSlot - 1;
         } else {
            destSlot = startSlot;
         }

         while (backwards ? destSlot >= startSlot : destSlot < endSlot) {
            Slot slot = this.slots.get(destSlot);
            ItemStack target = slot.getItem();
            if (target.isEmpty() && slot.mayPlace(itemStack)) {
               int maxStackSize = slot.getMaxStackSize(itemStack);
               slot.setByPlayer(itemStack.split(Math.min(itemStack.getCount(), maxStackSize)));
               slot.setChanged();
               anythingChanged = true;
               break;
            }

            if (backwards) {
               destSlot--;
            } else {
               destSlot++;
            }
         }
      }

      return anythingChanged;
   }

   public static int getQuickcraftType(final int mask) {
      return mask >> 2 & 3;
   }

   public static int getQuickcraftHeader(final int mask) {
      return mask & 3;
   }

   public static int getQuickcraftMask(final int header, final int type) {
      return header & 3 | (type & 3) << 2;
   }

   public static boolean isValidQuickcraftType(final int type, final Player player) {
      if (type == 0) {
         return true;
      } else {
         return type == 1 ? true : type == 2 && player.hasInfiniteMaterials();
      }
   }

   protected void resetQuickCraft() {
      this.quickcraftStatus = 0;
      this.quickcraftSlots.clear();
   }

   public static boolean canItemQuickReplace(final @Nullable Slot slot, final ItemStack itemStack, final boolean ignoreSize) {
      boolean slotIsEmpty = slot == null || !slot.hasItem();
      return !slotIsEmpty && ItemStack.isSameItemSameComponents(itemStack, slot.getItem())
         ? slot.getItem().getCount() + (ignoreSize ? 0 : itemStack.getCount()) <= itemStack.getMaxStackSize()
         : slotIsEmpty;
   }

   public static int getQuickCraftPlaceCount(final int quickCraftSlotsSize, final int quickCraftingType, final ItemStack itemStack) {
      return switch (quickCraftingType) {
         case 0 -> Mth.floor((float)itemStack.getCount() / quickCraftSlotsSize);
         case 1 -> 1;
         case 2 -> itemStack.getMaxStackSize();
         default -> itemStack.getCount();
      };
   }

   public boolean canDragTo(final Slot slot) {
      return true;
   }

   public static int getRedstoneSignalFromBlockEntity(final @Nullable BlockEntity blockEntity) {
      return blockEntity instanceof Container container ? getRedstoneSignalFromContainer(container) : 0;
   }

   public static int getRedstoneSignalFromContainer(final @Nullable Container container) {
      if (container == null) {
         return 0;
      }

      float totalPercent = 0.0F;

      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack itemStack = container.getItem(i);
         if (!itemStack.isEmpty()) {
            totalPercent += (float)itemStack.getCount() / container.getMaxStackSize(itemStack);
         }
      }

      totalPercent /= container.getContainerSize();
      return Mth.lerpDiscrete(totalPercent, 0, 15);
   }

   public void setCarried(final ItemStack carried) {
      this.carried = carried;
   }

   public ItemStack getCarried() {
      return this.carried;
   }

   public void suppressRemoteUpdates() {
      this.suppressRemoteUpdates = true;
   }

   public void resumeRemoteUpdates() {
      this.suppressRemoteUpdates = false;
   }

   public void transferState(final AbstractContainerMenu otherContainer) {
      Table<Container, Integer, Integer> otherSlots = HashBasedTable.create();

      for (int slotIndex = 0; slotIndex < otherContainer.slots.size(); slotIndex++) {
         Slot slot = otherContainer.slots.get(slotIndex);
         otherSlots.put(slot.container, slot.getContainerSlot(), slotIndex);
      }

      for (int slotIndex = 0; slotIndex < this.slots.size(); slotIndex++) {
         Slot slot = this.slots.get(slotIndex);
         Integer otherSlotIndex = (Integer)otherSlots.get(slot.container, slot.getContainerSlot());
         if (otherSlotIndex != null) {
            this.lastSlots.set(slotIndex, otherContainer.lastSlots.get(otherSlotIndex));
            RemoteSlot sourceRemoteSlot = otherContainer.remoteSlots.get(otherSlotIndex);
            RemoteSlot targetRemoteSlot = this.remoteSlots.get(slotIndex);
            if (sourceRemoteSlot instanceof RemoteSlot.Synchronized synchronizedSource
               && targetRemoteSlot instanceof RemoteSlot.Synchronized synchronizedTarget) {
               synchronizedTarget.copyFrom(synchronizedSource);
            }
         }
      }
   }

   public OptionalInt findSlot(final Container inventory, final int slotIndex) {
      for (int i = 0; i < this.slots.size(); i++) {
         Slot slot = this.slots.get(i);
         if (slot.container == inventory && slotIndex == slot.getContainerSlot()) {
            return OptionalInt.of(i);
         }
      }

      return OptionalInt.empty();
   }

   public int getStateId() {
      return this.stateId;
   }

   public int incrementStateId() {
      this.stateId = this.stateId + 1 & 32767;
      return this.stateId;
   }
}
