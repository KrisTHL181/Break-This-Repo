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

   protected AbstractMinecartContainer(EntityType<?> p_458598_, Level p_451360_) {
      super(p_458598_, p_451360_);
   }

   @Override
   public void destroy(ServerLevel p_456184_, DamageSource p_453155_) {
      super.destroy(p_456184_, p_453155_);
      this.chestVehicleDestroyed(p_453155_, p_456184_, this);
   }

   @Override
   public ItemStack getItem(int p_458498_) {
      return this.getChestVehicleItem(p_458498_);
   }

   @Override
   public ItemStack removeItem(int p_457872_, int p_450558_) {
      return this.removeChestVehicleItem(p_457872_, p_450558_);
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_458605_) {
      return this.removeChestVehicleItemNoUpdate(p_458605_);
   }

   @Override
   public void setItem(int p_454119_, ItemStack p_459463_) {
      this.setChestVehicleItem(p_454119_, p_459463_);
   }

   @Override
   public SlotAccess getSlot(int p_455459_) {
      return this.getChestVehicleSlot(p_455459_);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(Player p_457351_) {
      return this.isChestVehicleStillValid(p_457351_);
   }

   @Override
   public void remove(Entity.RemovalReason p_454115_) {
      if (!this.level().isClientSide() && p_454115_.shouldDestroy()) {
         Containers.dropContents(this.level(), this, this);
      }

      super.remove(p_454115_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_452089_) {
      super.addAdditionalSaveData(p_452089_);
      this.addChestVehicleSaveData(p_452089_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_451805_) {
      super.readAdditionalSaveData(p_451805_);
      this.readChestVehicleSaveData(p_451805_);
   }

   @Override
   public InteractionResult interact(Player p_452542_, InteractionHand p_458127_) {
      return this.interactWithContainerVehicle(p_452542_);
   }

   @Override
   protected Vec3 applyNaturalSlowdown(Vec3 p_452055_) {
      float f = 0.98F;
      if (this.lootTable == null) {
         int i = 15 - AbstractContainerMenu.getRedstoneSignalFromContainer(this);
         f += i * 0.001F;
      }

      if (this.isInWater()) {
         f *= 0.95F;
      }

      return p_452055_.multiply(f, 0.0, f);
   }

   @Override
   public void clearContent() {
      this.clearChestVehicleContent();
   }

   public void setLootTable(ResourceKey<LootTable> p_455187_, long p_459113_) {
      this.lootTable = p_455187_;
      this.lootTableSeed = p_459113_;
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(int p_452235_, Inventory p_452809_, Player p_460397_) {
      if (this.lootTable != null && p_460397_.isSpectator()) {
         return null;
      }

      this.unpackChestVehicleLootTable(p_452809_.player);
      return this.createMenu(p_452235_, p_452809_);
   }

   protected abstract AbstractContainerMenu createMenu(int var1, Inventory var2);

   @Override
   public @Nullable ResourceKey<LootTable> getContainerLootTable() {
      return this.lootTable;
   }

   @Override
   public void setContainerLootTable(@Nullable ResourceKey<LootTable> p_454651_) {
      this.lootTable = p_454651_;
   }

   @Override
   public long getContainerLootTableSeed() {
      return this.lootTableSeed;
   }

   @Override
   public void setContainerLootTableSeed(long p_459178_) {
      this.lootTableSeed = p_459178_;
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
