package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ShelfBlockEntity extends BlockEntity implements ListBackedContainer, ItemOwner {
   public static final int MAX_ITEMS = 3;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String ALIGN_ITEMS_TO_BOTTOM_TAG = "align_items_to_bottom";
   private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
   private boolean alignItemsToBottom;

   public ShelfBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.SHELF, worldPosition, blockState);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.items.clear();
      ContainerHelper.loadAllItems(input, this.items);
      this.alignItemsToBottom = input.getBooleanOr("align_items_to_bottom", false);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, this.items, true);
      output.putBoolean("align_items_to_bottom", this.alignItemsToBottom);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
         ContainerHelper.saveAllItems(output, this.items, true);
         output.putBoolean("align_items_to_bottom", this.alignItemsToBottom);
         return output.buildResult();
      }
   }

   @Override
   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   public boolean stillValid(final Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   public ItemStack swapItemNoUpdate(final int slot, final ItemStack heldItemStack) {
      ItemStack retrievedItem = this.removeItemNoUpdate(slot);
      this.setItemNoUpdate(slot, heldItemStack);
      return retrievedItem;
   }

   public void setChanged(final Holder.@Nullable Reference<GameEvent> event) {
      super.setChanged();
      if (this.level != null) {
         if (event != null) {
            this.level.gameEvent(event, this.worldPosition, GameEvent.Context.of(this.getBlockState()));
         }

         this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }

   @Override
   public void setChanged() {
      this.setChanged(GameEvent.BLOCK_ACTIVATE);
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      super.applyImplicitComponents(components);
      components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
   }

   @Override
   protected void collectImplicitComponents(final DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
   }

   @Override
   public void removeComponentsFromTag(final ValueOutput output) {
      output.discard("Items");
   }

   @Override
   public Level level() {
      return this.level;
   }

   @Override
   public Vec3 position() {
      return Vec3.atCenterOf(this.getBlockPos());
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.getBlockState().getValue(ShelfBlock.FACING).getOpposite().toYRot();
   }

   public boolean getAlignItemsToBottom() {
      return this.alignItemsToBottom;
   }
}
