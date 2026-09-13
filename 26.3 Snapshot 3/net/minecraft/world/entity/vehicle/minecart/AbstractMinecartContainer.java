package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMinecartContainer extends AbstractMinecart implements ContainerEntity {
   private NonNullList<ItemStack> itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
   private @Nullable ResourceKey<LootTable> lootTable;
   private long lootTableSeed;

   protected AbstractMinecartContainer(final EntityType<?> type, final Level level) {
      super(type, level);
   }

   @Override
   public void destroy(final ServerLevel level, final DamageSource source) {
      super.destroy(level, source);
      this.chestVehicleDestroyed(source, level, this);
   }

   @Override
   public ItemStack getItem(final int slot) {
      return this.getChestVehicleItem(slot);
   }

   @Override
   public ItemStack removeItem(final int slot, final int count) {
      return this.removeChestVehicleItem(slot, count);
   }

   @Override
   public ItemStack removeItemNoUpdate(final int slot) {
      return this.removeChestVehicleItemNoUpdate(slot);
   }

   @Override
   public void setItem(final int slot, final ItemStack itemStack) {
      this.setChestVehicleItem(slot, itemStack);
   }

   @Override
   public SlotAccess getSlot(final int slot) {
      return this.getChestVehicleSlot(slot);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.isChestVehicleStillValid(player);
   }

   @Override
   public void remove(final Entity.RemovalReason reason) {
      if (!this.level().isClientSide() && reason.shouldDestroy()) {
         Containers.dropContents(this.level(), this, this);
      }

      super.remove(reason);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      this.addChestVehicleSaveData(output);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.readChestVehicleSaveData(input);
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      return this.interactWithContainerVehicle(player);
   }

   @Override
   protected Vec3 applyNaturalSlowdown(final Vec3 deltaMovement) {
      float keep = 0.98F;
      if (this.lootTable == null) {
         int emptiness = 15 - AbstractContainerMenu.getRedstoneSignalFromContainer(this);
         keep += emptiness * 0.001F;
      }

      if (this.isInWater()) {
         keep *= 0.95F;
      }

      return deltaMovement.multiply(keep, 0.0, keep);
   }

   @Override
   public void clearContent() {
      this.clearChestVehicleContent();
   }

   public void setLootTable(final ResourceKey<LootTable> lootTable, final long seed) {
      this.lootTable = lootTable;
      this.lootTableSeed = seed;
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
      if (this.lootTable != null && player.isSpectator()) {
         return null;
      }

      this.unpackChestVehicleLootTable(inventory.player);
      return this.createMenu(containerId, inventory);
   }

   protected abstract AbstractContainerMenu createMenu(final int containerId, final Inventory inventory);

   @Override
   public @Nullable ResourceKey<LootTable> getContainerLootTable() {
      return this.lootTable;
   }

   @Override
   public void setContainerLootTable(final @Nullable ResourceKey<LootTable> lootTable) {
      this.lootTable = lootTable;
   }

   @Override
   public long getContainerLootTableSeed() {
      return this.lootTableSeed;
   }

   @Override
   public void setContainerLootTableSeed(final long lootTableSeed) {
      this.lootTableSeed = lootTableSeed;
   }

   @Override
   public NonNullList<ItemStack> getItemStacks() {
      return this.itemStacks;
   }

   @Override
   public void clearItemStacks() {
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
   }
}
