package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jspecify.annotations.Nullable;

public class DecoratedPotBlockEntity extends BlockEntity implements RandomizableContainer, ContainerSingleItem.BlockContainerSingleItem {
   public static final String TAG_SHERDS = "sherds";
   public static final String TAG_ITEM = "item";
   public static final int EVENT_POT_WOBBLES = 1;
   public long wobbleStartedAtTick;
   public DecoratedPotBlockEntity.@Nullable WobbleStyle lastWobbleStyle;
   private PotDecorations decorations;
   private ItemStack item = ItemStack.EMPTY;
   protected @Nullable ResourceKey<LootTable> lootTable;
   protected long lootTableSeed;

   public DecoratedPotBlockEntity(BlockPos p_273660_, BlockState p_272831_) {
      super(BlockEntityType.DECORATED_POT, p_273660_, p_272831_);
      this.decorations = PotDecorations.EMPTY;
   }

   @Override
   protected void saveAdditional(ValueOutput p_407299_) {
      super.saveAdditional(p_407299_);
      if (!this.decorations.equals(PotDecorations.EMPTY)) {
         p_407299_.store("sherds", PotDecorations.CODEC, this.decorations);
      }

      if (!this.trySaveLootTable(p_407299_) && !this.item.isEmpty()) {
         p_407299_.store("item", ItemStack.CODEC, this.item);
      }
   }

   @Override
   protected void loadAdditional(ValueInput p_408095_) {
      super.loadAdditional(p_408095_);
      this.decorations = p_408095_.<PotDecorations>read("sherds", PotDecorations.CODEC).orElse(PotDecorations.EMPTY);
      if (!this.tryLoadLootTable(p_408095_)) {
         this.item = p_408095_.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      } else {
         this.item = ItemStack.EMPTY;
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_334226_) {
      return this.saveCustomOnly(p_334226_);
   }

   public Direction getDirection() {
      return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
   }

   public PotDecorations getDecorations() {
      return this.decorations;
   }

   public static ItemStack createDecoratedPotItem(PotDecorations p_331852_) {
      ItemStack itemstack = Items.DECORATED_POT.getDefaultInstance();
      itemstack.set(DataComponents.POT_DECORATIONS, p_331852_);
      return itemstack;
   }

   @Override
   public @Nullable ResourceKey<LootTable> getLootTable() {
      return this.lootTable;
   }

   @Override
   public void setLootTable(@Nullable ResourceKey<LootTable> p_334371_) {
      this.lootTable = p_334371_;
   }

   @Override
   public long getLootTableSeed() {
      return this.lootTableSeed;
   }

   @Override
   public void setLootTableSeed(long p_311200_) {
      this.lootTableSeed = p_311200_;
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_333422_) {
      super.collectImplicitComponents(p_333422_);
      p_333422_.set(DataComponents.POT_DECORATIONS, this.decorations);
      p_333422_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(this.item)));
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_395448_) {
      super.applyImplicitComponents(p_395448_);
      this.decorations = p_395448_.getOrDefault(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY);
      this.item = p_395448_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_407211_) {
      super.removeComponentsFromTag(p_407211_);
      p_407211_.discard("sherds");
      p_407211_.discard("item");
   }

   @Override
   public ItemStack getTheItem() {
      this.unpackLootTable(null);
      return this.item;
   }

   @Override
   public ItemStack splitTheItem(int p_313165_) {
      this.unpackLootTable(null);
      ItemStack itemstack = this.item.split(p_313165_);
      if (this.item.isEmpty()) {
         this.item = ItemStack.EMPTY;
      }

      return itemstack;
   }

   @Override
   public void setTheItem(ItemStack p_310130_) {
      this.unpackLootTable(null);
      this.item = p_310130_;
   }

   @Override
   public BlockEntity getContainerBlockEntity() {
      return this;
   }

   public void wobble(DecoratedPotBlockEntity.WobbleStyle p_312241_) {
      if (this.level != null && !this.level.isClientSide()) {
         this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, p_312241_.ordinal());
      }
   }

   @Override
   public boolean triggerEvent(int p_309634_, int p_310889_) {
      if (this.level != null && p_309634_ == 1 && p_310889_ >= 0 && p_310889_ < DecoratedPotBlockEntity.WobbleStyle.values().length) {
         this.wobbleStartedAtTick = this.level.getGameTime();
         this.lastWobbleStyle = DecoratedPotBlockEntity.WobbleStyle.values()[p_310889_];
         return true;
      } else {
         return super.triggerEvent(p_309634_, p_310889_);
      }
   }

   public enum WobbleStyle {
      POSITIVE(7),
      NEGATIVE(10);

      public final int duration;

      WobbleStyle(final int p_311481_) {
         this.duration = p_311481_;
      }
   }
}
