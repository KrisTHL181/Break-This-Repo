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

public class ShelfBlockEntity extends BlockEntity implements ItemOwner, ListBackedContainer {
   public static final int MAX_ITEMS = 3;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String ALIGN_ITEMS_TO_BOTTOM_TAG = "align_items_to_bottom";
   private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
   private boolean alignItemsToBottom;

   public ShelfBlockEntity(BlockPos p_430844_, BlockState p_425392_) {
      super(BlockEntityType.SHELF, p_430844_, p_425392_);
   }

   @Override
   protected void loadAdditional(ValueInput p_425078_) {
      super.loadAdditional(p_425078_);
      this.items.clear();
      ContainerHelper.loadAllItems(p_425078_, this.items);
      this.alignItemsToBottom = p_425078_.getBooleanOr("align_items_to_bottom", false);
   }

   @Override
   protected void saveAdditional(ValueOutput p_428556_) {
      super.saveAdditional(p_428556_);
      ContainerHelper.saveAllItems(p_428556_, this.items, true);
      p_428556_.putBoolean("align_items_to_bottom", this.alignItemsToBottom);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_423146_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_423146_);
         ContainerHelper.saveAllItems(tagvalueoutput, this.items, true);
         tagvalueoutput.putBoolean("align_items_to_bottom", this.alignItemsToBottom);
         return tagvalueoutput.buildResult();
      }
   }

   @Override
   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   public boolean stillValid(Player p_429368_) {
      return Container.stillValidBlockEntity(this, p_429368_);
   }

   public ItemStack swapItemNoUpdate(int p_429914_, ItemStack p_428003_) {
      ItemStack itemstack = this.removeItemNoUpdate(p_429914_);
      this.setItemNoUpdate(p_429914_, p_428003_);
      return itemstack;
   }

   public void setChanged(Holder.@Nullable Reference<GameEvent> p_425942_) {
      super.setChanged();
      if (this.level != null) {
         if (p_425942_ != null) {
            this.level.gameEvent(p_425942_, this.worldPosition, GameEvent.Context.of(this.getBlockState()));
         }

         this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }

   @Override
   public void setChanged() {
      this.setChanged(GameEvent.BLOCK_ACTIVATE);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_426426_) {
      super.applyImplicitComponents(p_426426_);
      p_426426_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_426984_) {
      super.collectImplicitComponents(p_426984_);
      p_426984_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_424412_) {
      p_424412_.discard("Items");
   }

   @Override
   public Level level() {
      return this.level;
   }

   @Override
   public Vec3 position() {
      return this.getBlockPos().getCenter();
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.getBlockState().getValue(ShelfBlock.FACING).getOpposite().toYRot();
   }

   public boolean getAlignItemsToBottom() {
      return this.alignItemsToBottom;
   }
}
