package net.minecraft.world.entity.vehicle;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface ContainerEntity extends Container, MenuProvider {
   Vec3 position();

   AABB getBoundingBox();

   @Nullable ResourceKey<LootTable> getContainerLootTable();

   void setContainerLootTable(final @Nullable ResourceKey<LootTable> lootTable);

   long getContainerLootTableSeed();

   void setContainerLootTableSeed(final long lootTableSeed);

   NonNullList<ItemStack> getItemStacks();

   void clearItemStacks();

   Level level();

   boolean isRemoved();

   @Override
   default boolean isEmpty() {
      return this.isChestVehicleEmpty();
   }

   default void addChestVehicleSaveData(final ValueOutput output) {
      if (this.getContainerLootTable() != null) {
         output.putString("LootTable", this.getContainerLootTable().identifier().toString());
         if (this.getContainerLootTableSeed() != 0L) {
            output.putLong("LootTableSeed", this.getContainerLootTableSeed());
         }
      } else {
         ContainerHelper.saveAllItems(output, this.getItemStacks());
      }
   }

   default void readChestVehicleSaveData(final ValueInput input) {
      this.clearItemStacks();
      ResourceKey<LootTable> lootTable = input.<ResourceKey<LootTable>>read("LootTable", LootTable.KEY_CODEC).orElse(null);
      this.setContainerLootTable(lootTable);
      this.setContainerLootTableSeed(input.getLongOr("LootTableSeed", 0L));
      if (lootTable == null) {
         ContainerHelper.loadAllItems(input, this.getItemStacks());
      }
   }

   default void chestVehicleDestroyed(final DamageSource source, final ServerLevel level, final Entity entity) {
      if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
         Containers.dropContents(level, entity, this);
         if (source.getDirectEntity() instanceof Player player) {
            PiglinAi.angerNearbyPiglins(level, player, true);
         }
      }
   }

   default InteractionResult interactWithContainerVehicle(final Player player) {
      player.openMenu(this);
      return InteractionResult.SUCCESS;
   }

   default void unpackChestVehicleLootTable(final @Nullable Player player) {
      MinecraftServer server = this.level().getServer();
      if (this.getContainerLootTable() != null && server != null) {
         LootTable lootTable = server.reloadableRegistries().getLootTable(this.getContainerLootTable());
         if (player != null) {
            CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)player, this.getContainerLootTable());
         }

         this.setContainerLootTable(null);
         LootParams.Builder builder = new LootParams.Builder((ServerLevel)this.level()).withParameter(LootContextParams.ORIGIN, this.position());
         if (player != null) {
            builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
         }

         lootTable.fill(this, builder.create(LootContextParamSets.CHEST), this.getContainerLootTableSeed());
      }
   }

   default void clearChestVehicleContent() {
      this.unpackChestVehicleLootTable(null);
      this.getItemStacks().clear();
   }

   default boolean isChestVehicleEmpty() {
      for (ItemStack itemStack : this.getItemStacks()) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   default ItemStack removeChestVehicleItemNoUpdate(final int slot) {
      this.unpackChestVehicleLootTable(null);
      ItemStack itemStack = this.getItemStacks().get(slot);
      if (itemStack.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.getItemStacks().set(slot, ItemStack.EMPTY);
      return itemStack;
   }

   default ItemStack getChestVehicleItem(final int slot) {
      this.unpackChestVehicleLootTable(null);
      return this.getItemStacks().get(slot);
   }

   default ItemStack removeChestVehicleItem(final int slot, final int count) {
      this.unpackChestVehicleLootTable(null);
      return ContainerHelper.removeItem(this.getItemStacks(), slot, count);
   }

   default void setChestVehicleItem(final int slot, final ItemStack itemStack) {
      this.unpackChestVehicleLootTable(null);
      this.getItemStacks().set(slot, itemStack);
      itemStack.limitSize(this.getMaxStackSize(itemStack));
   }

   default @Nullable SlotAccess getChestVehicleSlot(final int slot) {
      return slot >= 0 && slot < this.getContainerSize() ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return ContainerEntity.this.getChestVehicleItem(slot);
         }

         @Override
         public boolean set(final ItemStack itemStack) {
            ContainerEntity.this.setChestVehicleItem(slot, itemStack);
            return true;
         }
      } : null;
   }

   default boolean isChestVehicleStillValid(final Player player) {
      return !this.isRemoved() && player.isWithinEntityInteractionRange(this.getBoundingBox(), 4.0);
   }
}
