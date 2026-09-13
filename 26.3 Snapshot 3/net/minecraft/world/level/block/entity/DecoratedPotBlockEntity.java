package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jspecify.annotations.Nullable;

public class DecoratedPotBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem, RandomizableContainer {
   public static final String TAG_SHERDS = "sherds";
   public static final String TAG_ITEM = "item";
   public static final int EVENT_POT_WOBBLES = 1;
   public long wobbleStartedAtTick;
   public DecoratedPotBlockEntity.@Nullable WobbleStyle lastWobbleStyle;
   private PotDecorations decorations;
   private ItemStack item = ItemStack.EMPTY;
   protected @Nullable ResourceKey<LootTable> lootTable;
   protected long lootTableSeed;

   public DecoratedPotBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.DECORATED_POT, worldPosition, blockState);
      this.decorations = PotDecorations.EMPTY;
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (!this.decorations.equals(PotDecorations.EMPTY)) {
         output.store("sherds", PotDecorations.CODEC, this.decorations);
      }

      if (!this.trySaveLootTable(output) && !this.item.isEmpty()) {
         output.store("item", ItemStack.CODEC, this.item);
      }
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.decorations = input.<PotDecorations>read("sherds", PotDecorations.CODEC).orElse(PotDecorations.EMPTY);
      if (!this.tryLoadLootTable(input)) {
         this.item = input.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      } else {
         this.item = ItemStack.EMPTY;
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public Direction getDirection() {
      return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
   }

   public PotDecorations getDecorations() {
      return this.decorations;
   }

   public static ItemStackTemplate createDecoratedPotTemplate(final PotDecorations decorations) {
      return new ItemStackTemplate(Items.DECORATED_POT, DataComponentPatch.builder().set(DataComponents.POT_DECORATIONS, decorations).build());
   }

   public static ItemStack createDecoratedPotInstance(final PotDecorations decorations) {
      return createDecoratedPotTemplate(decorations).create();
   }

   @Override
   public @Nullable ResourceKey<LootTable> getLootTable() {
      return this.lootTable;
   }

   @Override
   public void setLootTable(final @Nullable ResourceKey<LootTable> lootTable) {
      this.lootTable = lootTable;
   }

   @Override
   public long getLootTableSeed() {
      return this.lootTableSeed;
   }

   @Override
   public void setLootTableSeed(final long lootTableSeed) {
      this.lootTableSeed = lootTableSeed;
   }

   @Override
   protected void collectImplicitComponents(final DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.POT_DECORATIONS, this.decorations);
      components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(this.item)));
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      super.applyImplicitComponents(components);
      this.decorations = components.getOrDefault(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY);
      this.item = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
   }

   @Override
   public void removeComponentsFromTag(final ValueOutput output) {
      super.removeComponentsFromTag(output);
      output.discard("sherds");
      output.discard("item");
   }

   @Override
   public ItemStack getTheItem() {
      this.unpackLootTable(null);
      return this.item;
   }

   @Override
   public ItemStack splitTheItem(final int count) {
      this.unpackLootTable(null);
      ItemStack result = this.item.split(count);
      if (this.item.isEmpty()) {
         this.item = ItemStack.EMPTY;
      }

      return result;
   }

   @Override
   public void setTheItem(final ItemStack itemStack) {
      this.unpackLootTable(null);
      this.item = itemStack;
   }

   @Override
   public BlockEntity getContainerBlockEntity() {
      return this;
   }

   public void wobble(final DecoratedPotBlockEntity.WobbleStyle wobbleStyle) {
      if (this.level != null && !this.level.isClientSide()) {
         this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, wobbleStyle.ordinal());
      }
   }

   @Override
   public boolean triggerEvent(final int event, final int data) {
      if (this.level != null && event == 1 && data >= 0 && data < DecoratedPotBlockEntity.WobbleStyle.values().length) {
         this.wobbleStartedAtTick = this.level.getGameTime();
         this.lastWobbleStyle = DecoratedPotBlockEntity.WobbleStyle.values()[data];
         return true;
      } else {
         return super.triggerEvent(event, data);
      }
   }

   public enum WobbleStyle {
      POSITIVE(7),
      NEGATIVE(10);

      public final int duration;

      WobbleStyle(final int duration) {
         this.duration = duration;
      }
   }
}
