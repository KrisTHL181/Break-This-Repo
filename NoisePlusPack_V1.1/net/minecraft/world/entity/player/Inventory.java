package net.minecraft.world.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Inventory implements Container, Nameable {
   public static final int POP_TIME_DURATION = 5;
   public static final int INVENTORY_SIZE = 36;
   public static final int SELECTION_SIZE = 9;
   public static final int SLOT_OFFHAND = 40;
   public static final int SLOT_BODY_ARMOR = 41;
   public static final int SLOT_SADDLE = 42;
   public static final int NOT_FOUND_INDEX = -1;
   public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOT_MAPPING = new Int2ObjectArrayMap(
      Map.of(
         EquipmentSlot.FEET.getIndex(36),
         EquipmentSlot.FEET,
         EquipmentSlot.LEGS.getIndex(36),
         EquipmentSlot.LEGS,
         EquipmentSlot.CHEST.getIndex(36),
         EquipmentSlot.CHEST,
         EquipmentSlot.HEAD.getIndex(36),
         EquipmentSlot.HEAD,
         40,
         EquipmentSlot.OFFHAND,
         41,
         EquipmentSlot.BODY,
         42,
         EquipmentSlot.SADDLE
      )
   );
   private static final Component DEFAULT_NAME = Component.translatable("container.inventory");
   private final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
   private int selected;
   public final Player player;
   private final EntityEquipment equipment;
   private int timesChanged;

   public Inventory(Player p_35983_, EntityEquipment p_392430_) {
      this.player = p_35983_;
      this.equipment = p_392430_;
   }

   public int getSelectedSlot() {
      return this.selected;
   }

   public void setSelectedSlot(int p_398009_) {
      if (!isHotbarSlot(p_398009_)) {
         throw new IllegalArgumentException("Invalid selected slot");
      }

      this.selected = p_398009_;
   }

   public ItemStack getSelectedItem() {
      return this.items.get(this.selected);
   }

   public ItemStack setSelectedItem(ItemStack p_393963_) {
      return this.items.set(this.selected, p_393963_);
   }

   public static int getSelectionSize() {
      return 9;
   }

   public NonNullList<ItemStack> getNonEquipmentItems() {
      return this.items;
   }

   private boolean hasRemainingSpaceForItem(ItemStack p_36015_, ItemStack p_36016_) {
      return !p_36015_.isEmpty()
         && ItemStack.isSameItemSameComponents(p_36015_, p_36016_)
         && p_36015_.isStackable()
         && p_36015_.getCount() < this.getMaxStackSize(p_36015_);
   }

   public int getFreeSlot() {
      for (int i = 0; i < this.items.size(); i++) {
         if (this.items.get(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public void addAndPickItem(ItemStack p_378587_) {
      this.setSelectedSlot(this.getSuitableHotbarSlot());
      if (!this.items.get(this.selected).isEmpty()) {
         int i = this.getFreeSlot();
         if (i != -1) {
            this.items.set(i, this.items.get(this.selected));
         }
      }

      this.items.set(this.selected, p_378587_);
   }

   public void pickSlot(int p_36039_) {
      this.setSelectedSlot(this.getSuitableHotbarSlot());
      ItemStack itemstack = this.items.get(this.selected);
      this.items.set(this.selected, this.items.get(p_36039_));
      this.items.set(p_36039_, itemstack);
   }

   public static boolean isHotbarSlot(int p_36046_) {
      return p_36046_ >= 0 && p_36046_ < 9;
   }

   public int findSlotMatchingItem(ItemStack p_36031_) {
      for (int i = 0; i < this.items.size(); i++) {
         if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameComponents(p_36031_, this.items.get(i))) {
            return i;
         }
      }

      return -1;
   }

   public static boolean isUsableForCrafting(ItemStack p_362871_) {
      return !p_362871_.isDamaged() && !p_362871_.isEnchanted() && !p_362871_.has(DataComponents.CUSTOM_NAME);
   }

   public int findSlotMatchingCraftingIngredient(Holder<Item> p_363996_, ItemStack p_376934_) {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemstack = this.items.get(i);
         if (!itemstack.isEmpty()
            && itemstack.is(p_363996_)
            && isUsableForCrafting(itemstack)
            && (p_376934_.isEmpty() || ItemStack.isSameItemSameComponents(p_376934_, itemstack))) {
            return i;
         }
      }

      return -1;
   }

   public int getSuitableHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         int j = (this.selected + i) % 9;
         if (this.items.get(j).isEmpty()) {
            return j;
         }
      }

      for (int k = 0; k < 9; k++) {
         int l = (this.selected + k) % 9;
         if (!this.items.get(l).isEnchanted()) {
            return l;
         }
      }

      return this.selected;
   }

   public int clearOrCountMatchingItems(Predicate<ItemStack> p_36023_, int p_36024_, Container p_36025_) {
      int i = 0;
      boolean flag = p_36024_ == 0;
      i += ContainerHelper.clearOrCountMatchingItems(this, p_36023_, p_36024_ - i, flag);
      i += ContainerHelper.clearOrCountMatchingItems(p_36025_, p_36023_, p_36024_ - i, flag);
      ItemStack itemstack = this.player.containerMenu.getCarried();
      i += ContainerHelper.clearOrCountMatchingItems(itemstack, p_36023_, p_36024_ - i, flag);
      if (itemstack.isEmpty()) {
         this.player.containerMenu.setCarried(ItemStack.EMPTY);
      }

      return i;
   }

   private int addResource(ItemStack p_36067_) {
      int i = this.getSlotWithRemainingSpace(p_36067_);
      if (i == -1) {
         i = this.getFreeSlot();
      }

      return i == -1 ? p_36067_.getCount() : this.addResource(i, p_36067_);
   }

   private int addResource(int p_36048_, ItemStack p_36049_) {
      int i = p_36049_.getCount();
      ItemStack itemstack = this.getItem(p_36048_);
      if (itemstack.isEmpty()) {
         itemstack = p_36049_.copyWithCount(0);
         this.setItem(p_36048_, itemstack);
      }

      int j = this.getMaxStackSize(itemstack) - itemstack.getCount();
      int k = Math.min(i, j);
      if (k == 0) {
         return i;
      }

      i -= k;
      itemstack.grow(k);
      itemstack.setPopTime(5);
      return i;
   }

   public int getSlotWithRemainingSpace(ItemStack p_36051_) {
      if (this.hasRemainingSpaceForItem(this.getItem(this.selected), p_36051_)) {
         return this.selected;
      }

      if (this.hasRemainingSpaceForItem(this.getItem(40), p_36051_)) {
         return 40;
      }

      for (int i = 0; i < this.items.size(); i++) {
         if (this.hasRemainingSpaceForItem(this.items.get(i), p_36051_)) {
            return i;
         }
      }

      return -1;
   }

   public void tick() {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemstack = this.getItem(i);
         if (!itemstack.isEmpty()) {
            itemstack.inventoryTick(this.player.level(), this.player, i == this.selected ? EquipmentSlot.MAINHAND : null);
         }
      }
   }

   public boolean add(ItemStack p_36055_) {
      return this.add(-1, p_36055_);
   }

   public boolean add(int p_36041_, ItemStack p_36042_) {
      if (p_36042_.isEmpty()) {
         return false;
      }

      try {
         if (p_36042_.isDamaged()) {
            if (p_36041_ == -1) {
               p_36041_ = this.getFreeSlot();
            }

            if (p_36041_ >= 0) {
               this.items.set(p_36041_, p_36042_.copyAndClear());
               this.items.get(p_36041_).setPopTime(5);
               return true;
            } else if (this.player.hasInfiniteMaterials()) {
               p_36042_.setCount(0);
               return true;
            } else {
               return false;
            }
         } else {
            int i;
            do {
               i = p_36042_.getCount();
               if (p_36041_ == -1) {
                  p_36042_.setCount(this.addResource(p_36042_));
               } else {
                  p_36042_.setCount(this.addResource(p_36041_, p_36042_));
               }
            } while (!p_36042_.isEmpty() && p_36042_.getCount() < i);

            if (p_36042_.getCount() == i && this.player.hasInfiniteMaterials()) {
               p_36042_.setCount(0);
               return true;
            } else {
               return p_36042_.getCount() < i;
            }
         }
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Adding item to inventory");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Item being added");
         crashreportcategory.setDetail("Item ID", Item.getId(p_36042_.getItem()));
         crashreportcategory.setDetail("Item data", p_36042_.getDamageValue());
         crashreportcategory.setDetail("Item name", () -> p_36042_.getHoverName().getString());
         throw new ReportedException(crashreport);
      }
   }

   public void placeItemBackInInventory(ItemStack p_150080_) {
      this.placeItemBackInInventory(p_150080_, true);
   }

   public void placeItemBackInInventory(ItemStack p_150077_, boolean p_150078_) {
      while (!p_150077_.isEmpty()) {
         int i = this.getSlotWithRemainingSpace(p_150077_);
         if (i == -1) {
            i = this.getFreeSlot();
         }

         if (i == -1) {
            this.player.drop(p_150077_, false);
            break;
         }

         int j = p_150077_.getMaxStackSize() - this.getItem(i).getCount();
         if (this.add(i, p_150077_.split(j)) && p_150078_ && this.player instanceof ServerPlayer serverplayer) {
            serverplayer.connection.send(this.createInventoryUpdatePacket(i));
         }
      }
   }

   public ClientboundSetPlayerInventoryPacket createInventoryUpdatePacket(int p_362278_) {
      return new ClientboundSetPlayerInventoryPacket(p_362278_, this.getItem(p_362278_).copy());
   }

   @Override
   public ItemStack removeItem(int p_35993_, int p_35994_) {
      if (p_35993_ < this.items.size()) {
         return ContainerHelper.removeItem(this.items, p_35993_, p_35994_);
      }

      EquipmentSlot equipmentslot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(p_35993_);
      if (equipmentslot != null) {
         ItemStack itemstack = this.equipment.get(equipmentslot);
         if (!itemstack.isEmpty()) {
            return itemstack.split(p_35994_);
         }
      }

      return ItemStack.EMPTY;
   }

   public void removeItem(ItemStack p_36058_) {
      for (int i = 0; i < this.items.size(); i++) {
         if (this.items.get(i) == p_36058_) {
            this.items.set(i, ItemStack.EMPTY);
            return;
         }
      }

      ObjectIterator var5 = EQUIPMENT_SLOT_MAPPING.values().iterator();

      while (var5.hasNext()) {
         EquipmentSlot equipmentslot = (EquipmentSlot)var5.next();
         ItemStack itemstack = this.equipment.get(equipmentslot);
         if (itemstack == p_36058_) {
            this.equipment.set(equipmentslot, ItemStack.EMPTY);
            return;
         }
      }
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_36029_) {
      if (p_36029_ < this.items.size()) {
         ItemStack itemstack = this.items.get(p_36029_);
         this.items.set(p_36029_, ItemStack.EMPTY);
         return itemstack;
      } else {
         EquipmentSlot equipmentslot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(p_36029_);
         return equipmentslot != null ? this.equipment.set(equipmentslot, ItemStack.EMPTY) : ItemStack.EMPTY;
      }
   }

   @Override
   public void setItem(int p_35999_, ItemStack p_36000_) {
      if (p_35999_ < this.items.size()) {
         this.items.set(p_35999_, p_36000_);
      }

      EquipmentSlot equipmentslot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(p_35999_);
      if (equipmentslot != null) {
         this.equipment.set(equipmentslot, p_36000_);
      }
   }

   public void save(ValueOutput.TypedOutputList<ItemStackWithSlot> p_406529_) {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemstack = this.items.get(i);
         if (!itemstack.isEmpty()) {
            p_406529_.add(new ItemStackWithSlot(i, itemstack));
         }
      }
   }

   public void load(ValueInput.TypedInputList<ItemStackWithSlot> p_409752_) {
      this.items.clear();

      for (ItemStackWithSlot itemstackwithslot : p_409752_) {
         if (itemstackwithslot.isValidInContainer(this.items.size())) {
            this.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
         }
      }
   }

   @Override
   public int getContainerSize() {
      return this.items.size() + EQUIPMENT_SLOT_MAPPING.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemstack : this.items) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      ObjectIterator var3 = EQUIPMENT_SLOT_MAPPING.values().iterator();

      while (var3.hasNext()) {
         EquipmentSlot equipmentslot = (EquipmentSlot)var3.next();
         if (!this.equipment.get(equipmentslot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int p_35991_) {
      if (p_35991_ < this.items.size()) {
         return this.items.get(p_35991_);
      }

      EquipmentSlot equipmentslot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(p_35991_);
      return equipmentslot != null ? this.equipment.get(equipmentslot) : ItemStack.EMPTY;
   }

   @Override
   public Component getName() {
      return DEFAULT_NAME;
   }

   public void dropAll() {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack itemstack = this.items.get(i);
         if (!itemstack.isEmpty()) {
            this.player.drop(itemstack, true, false);
            this.items.set(i, ItemStack.EMPTY);
         }
      }

      this.equipment.dropAll(this.player);
   }

   @Override
   public void setChanged() {
      this.timesChanged++;
   }

   public int getTimesChanged() {
      return this.timesChanged;
   }

   @Override
   public boolean stillValid(Player p_36009_) {
      return true;
   }

   public boolean contains(ItemStack p_36064_) {
      for (ItemStack itemstack : this) {
         if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, p_36064_)) {
            return true;
         }
      }

      return false;
   }

   public boolean contains(TagKey<Item> p_204076_) {
      for (ItemStack itemstack : this) {
         if (!itemstack.isEmpty() && itemstack.is(p_204076_)) {
            return true;
         }
      }

      return false;
   }

   public boolean contains(Predicate<ItemStack> p_332183_) {
      for (ItemStack itemstack : this) {
         if (p_332183_.test(itemstack)) {
            return true;
         }
      }

      return false;
   }

   public void replaceWith(Inventory p_36007_) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         this.setItem(i, p_36007_.getItem(i));
      }

      this.setSelectedSlot(p_36007_.getSelectedSlot());
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.equipment.clear();
   }

   public void fillStackedContents(StackedItemContents p_364670_) {
      for (ItemStack itemstack : this.items) {
         p_364670_.accountSimpleStack(itemstack);
      }
   }

   public ItemStack removeFromSelected(boolean p_182404_) {
      ItemStack itemstack = this.getSelectedItem();
      return itemstack.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, p_182404_ ? itemstack.getCount() : 1);
   }
}
