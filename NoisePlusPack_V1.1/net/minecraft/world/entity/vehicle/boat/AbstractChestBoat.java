package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public abstract class AbstractChestBoat extends AbstractBoat implements HasCustomInventoryScreen, ContainerEntity {
   private static final int CONTAINER_SIZE = 27;
   private NonNullList<ItemStack> itemStacks = NonNullList.withSize(27, ItemStack.EMPTY);
   private @Nullable ResourceKey<LootTable> lootTable;
   private long lootTableSeed;

   public AbstractChestBoat(EntityType<? extends AbstractChestBoat> p_459229_, Level p_460860_, Supplier<Item> p_460245_) {
      super(p_459229_, p_460860_, p_460245_);
   }

   @Override
   protected float getSinglePassengerXOffset() {
      return 0.15F;
   }

   @Override
   protected int getMaxPassengers() {
      return 1;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_451064_) {
      super.addAdditionalSaveData(p_451064_);
      this.addChestVehicleSaveData(p_451064_);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_456965_) {
      super.readAdditionalSaveData(p_456965_);
      this.readChestVehicleSaveData(p_456965_);
   }

   @Override
   public void destroy(ServerLevel p_454094_, DamageSource p_458824_) {
      this.destroy(p_454094_, this.getDropItem());
      this.chestVehicleDestroyed(p_458824_, p_454094_, this);
   }

   @Override
   public void remove(Entity.RemovalReason p_459202_) {
      if (!this.level().isClientSide() && p_459202_.shouldDestroy()) {
         Containers.dropContents(this.level(), this, this);
      }

      super.remove(p_459202_);
   }

   @Override
   public InteractionResult interact(Player p_458305_, InteractionHand p_452194_) {
      InteractionResult interactionresult = super.interact(p_458305_, p_452194_);
      if (interactionresult != InteractionResult.PASS) {
         return interactionresult;
      }

      if (this.canAddPassenger(p_458305_) && !p_458305_.isSecondaryUseActive()) {
         return InteractionResult.PASS;
      }

      InteractionResult interactionresult1 = this.interactWithContainerVehicle(p_458305_);
      if (interactionresult1.consumesAction() && p_458305_.level() instanceof ServerLevel serverlevel) {
         this.gameEvent(GameEvent.CONTAINER_OPEN, p_458305_);
         PiglinAi.angerNearbyPiglins(serverlevel, p_458305_, true);
      }

      return interactionresult1;
   }

   @Override
   public void openCustomInventoryScreen(Player p_455682_) {
      p_455682_.openMenu(this);
      if (p_455682_.level() instanceof ServerLevel serverlevel) {
         this.gameEvent(GameEvent.CONTAINER_OPEN, p_455682_);
         PiglinAi.angerNearbyPiglins(serverlevel, p_455682_, true);
      }
   }

   @Override
   public void clearContent() {
      this.clearChestVehicleContent();
   }

   @Override
   public int getContainerSize() {
      return 27;
   }

   @Override
   public ItemStack getItem(int p_454668_) {
      return this.getChestVehicleItem(p_454668_);
   }

   @Override
   public ItemStack removeItem(int p_458614_, int p_457863_) {
      return this.removeChestVehicleItem(p_458614_, p_457863_);
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_453144_) {
      return this.removeChestVehicleItemNoUpdate(p_453144_);
   }

   @Override
   public void setItem(int p_458744_, ItemStack p_454793_) {
      this.setChestVehicleItem(p_458744_, p_454793_);
   }

   @Override
   public SlotAccess getSlot(int p_450768_) {
      return this.getChestVehicleSlot(p_450768_);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(Player p_458855_) {
      return this.isChestVehicleStillValid(p_458855_);
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(int p_452727_, Inventory p_455200_, Player p_454656_) {
      if (this.lootTable != null && p_454656_.isSpectator()) {
         return null;
      }

      this.unpackLootTable(p_455200_.player);
      return ChestMenu.threeRows(p_452727_, p_455200_, this);
   }

   public void unpackLootTable(@Nullable Player p_453726_) {
      this.unpackChestVehicleLootTable(p_453726_);
   }

   @Override
   public @Nullable ResourceKey<LootTable> getContainerLootTable() {
      return this.lootTable;
   }

   @Override
   public void setContainerLootTable(@Nullable ResourceKey<LootTable> p_456367_) {
      this.lootTable = p_456367_;
   }

   @Override
   public long getContainerLootTableSeed() {
      return this.lootTableSeed;
   }

   @Override
   public void setContainerLootTableSeed(long p_455055_) {
      this.lootTableSeed = p_455055_;
   }

   @Override
   public NonNullList<ItemStack> getItemStacks() {
      return this.itemStacks;
   }

   @Override
   public void clearItemStacks() {
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
   }

   @Override
   public void stopOpen(ContainerUser p_452104_) {
      this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(p_452104_.getLivingEntity()));
   }
}
