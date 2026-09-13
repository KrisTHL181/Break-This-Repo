package net.minecraft.world.entity.vehicle;

import net.minecraft.advancements.CriteriaTriggers;
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
import net.minecraft.world.entity.EntityType;
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

   void setContainerLootTable(@Nullable ResourceKey<LootTable> var1);

   long getContainerLootTableSeed();

   void setContainerLootTableSeed(long var1);

   NonNullList<ItemStack> getItemStacks();

   void clearItemStacks();

   Level level();

   boolean isRemoved();

   @Override
   default boolean isEmpty() {
      return this.isChestVehicleEmpty();
   }

   default void addChestVehicleSaveData(ValueOutput p_406568_) {
      if (this.getContainerLootTable() != null) {
         p_406568_.putString("LootTable", this.getContainerLootTable().identifier().toString());
         if (this.getContainerLootTableSeed() != 0L) {
            p_406568_.putLong("LootTableSeed", this.getContainerLootTableSeed());
         }
      } else {
         ContainerHelper.saveAllItems(p_406568_, this.getItemStacks());
      }
   }

   default void readChestVehicleSaveData(ValueInput p_407289_) {
      this.clearItemStacks();
      ResourceKey<LootTable> resourcekey = p_407289_.<ResourceKey<LootTable>>read("LootTable", LootTable.KEY_CODEC).orElse(null);
      this.setContainerLootTable(resourcekey);
      this.setContainerLootTableSeed(p_407289_.getLongOr("LootTableSeed", 0L));
      if (resourcekey == null) {
         ContainerHelper.loadAllItems(p_407289_, this.getItemStacks());
      }
   }

   default void chestVehicleDestroyed(DamageSource p_219928_, ServerLevel p_369535_, Entity p_219930_) {
      if (p_369535_.getGameRules().get(GameRules.ENTITY_DROPS)) {
         Containers.dropContents(p_369535_, p_219930_, this);
         Entity entity = p_219928_.getDirectEntity();
         if (entity != null && entity.getType() == EntityType.PLAYER) {
            PiglinAi.angerNearbyPiglins(p_369535_, (Player)entity, true);
         }
      }
   }

   default InteractionResult interactWithContainerVehicle(Player p_270068_) {
      p_270068_.openMenu(this);
      return InteractionResult.SUCCESS;
   }

   default void unpackChestVehicleLootTable(@Nullable Player p_219950_) {
      MinecraftServer minecraftserver = this.level().getServer();
      if (this.getContainerLootTable() != null && minecraftserver != null) {
         LootTable loottable = minecraftserver.reloadableRegistries().getLootTable(this.getContainerLootTable());
         if (p_219950_ != null) {
            CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)p_219950_, this.getContainerLootTable());
         }

         this.setContainerLootTable(null);
         LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel)this.level()).withParameter(LootContextParams.ORIGIN, this.position());
         if (p_219950_ != null) {
            lootparams$builder.withLuck(p_219950_.getLuck()).withParameter(LootContextParams.THIS_ENTITY, p_219950_);
         }

         loottable.fill(this, lootparams$builder.create(LootContextParamSets.CHEST), this.getContainerLootTableSeed());
      }
   }

   default void clearChestVehicleContent() {
      this.unpackChestVehicleLootTable(null);
      this.getItemStacks().clear();
   }

   default boolean isChestVehicleEmpty() {
      for (ItemStack itemstack : this.getItemStacks()) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   default ItemStack removeChestVehicleItemNoUpdate(int p_219946_) {
      this.unpackChestVehicleLootTable(null);
      ItemStack itemstack = this.getItemStacks().get(p_219946_);
      if (itemstack.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.getItemStacks().set(p_219946_, ItemStack.EMPTY);
      return itemstack;
   }

   default ItemStack getChestVehicleItem(int p_219948_) {
      this.unpackChestVehicleLootTable(null);
      return this.getItemStacks().get(p_219948_);
   }

   default ItemStack removeChestVehicleItem(int p_219937_, int p_219938_) {
      this.unpackChestVehicleLootTable(null);
      return ContainerHelper.removeItem(this.getItemStacks(), p_219937_, p_219938_);
   }

   default void setChestVehicleItem(int p_219941_, ItemStack p_219942_) {
      this.unpackChestVehicleLootTable(null);
      this.getItemStacks().set(p_219941_, p_219942_);
      p_219942_.limitSize(this.getMaxStackSize(p_219942_));
   }

   default @Nullable SlotAccess getChestVehicleSlot(final int p_219952_) {
      return p_219952_ >= 0 && p_219952_ < this.getContainerSize() ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return ContainerEntity.this.getChestVehicleItem(p_219952_);
         }

         @Override
         public boolean set(ItemStack p_219964_) {
            ContainerEntity.this.setChestVehicleItem(p_219952_, p_219964_);
            return true;
         }
      } : null;
   }

   default boolean isChestVehicleStillValid(Player p_219955_) {
      return !this.isRemoved() && p_219955_.isWithinEntityInteractionRange(this.getBoundingBox(), 4.0);
   }
}
