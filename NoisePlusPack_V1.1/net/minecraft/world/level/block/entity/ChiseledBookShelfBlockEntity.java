package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity extends BlockEntity implements ListBackedContainer {
   public static final int MAX_BOOKS_IN_STORAGE = 6;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_LAST_INTERACTED_SLOT = -1;
   private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
   private int lastInteractedSlot = -1;

   public ChiseledBookShelfBlockEntity(BlockPos p_249541_, BlockState p_251752_) {
      super(BlockEntityType.CHISELED_BOOKSHELF, p_249541_, p_251752_);
   }

   private void updateState(int p_261806_) {
      if (p_261806_ >= 0 && p_261806_ < 6) {
         this.lastInteractedSlot = p_261806_;
         BlockState blockstate = this.getBlockState();

         for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
            boolean flag = !this.getItem(i).isEmpty();
            BooleanProperty booleanproperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            blockstate = blockstate.setValue(booleanproperty, flag);
         }

         Objects.requireNonNull(this.level).setBlock(this.worldPosition, blockstate, 3);
         this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(blockstate));
      } else {
         LOGGER.error("Expected slot 0-5, got {}", p_261806_);
      }
   }

   @Override
   protected void loadAdditional(ValueInput p_410328_) {
      super.loadAdditional(p_410328_);
      this.items.clear();
      ContainerHelper.loadAllItems(p_410328_, this.items);
      this.lastInteractedSlot = p_410328_.getIntOr("last_interacted_slot", -1);
   }

   @Override
   protected void saveAdditional(ValueOutput p_406157_) {
      super.saveAdditional(p_406157_);
      ContainerHelper.saveAllItems(p_406157_, this.items, true);
      p_406157_.putInt("last_interacted_slot", this.lastInteractedSlot);
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean acceptsItemType(ItemStack p_423888_) {
      return p_423888_.is(ItemTags.BOOKSHELF_BOOKS);
   }

   @Override
   public ItemStack removeItem(int p_255828_, int p_255673_) {
      ItemStack itemstack = Objects.requireNonNullElse(this.getItems().get(p_255828_), ItemStack.EMPTY);
      this.getItems().set(p_255828_, ItemStack.EMPTY);
      if (!itemstack.isEmpty()) {
         this.updateState(p_255828_);
      }

      return itemstack;
   }

   @Override
   public void setItem(int p_256610_, ItemStack p_255789_) {
      if (this.acceptsItemType(p_255789_)) {
         this.getItems().set(p_256610_, p_255789_);
         this.updateState(p_256610_);
      } else if (p_255789_.isEmpty()) {
         this.removeItem(p_256610_, this.getMaxStackSize());
      }
   }

   @Override
   public boolean canTakeItem(Container p_282172_, int p_281387_, ItemStack p_283257_) {
      return p_282172_.hasAnyMatching(
         p_327306_ -> p_327306_.isEmpty()
            ? true
            : ItemStack.isSameItemSameComponents(p_283257_, p_327306_) && p_327306_.getCount() + p_283257_.getCount() <= p_282172_.getMaxStackSize(p_327306_)
      );
   }

   @Override
   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   public boolean stillValid(Player p_256481_) {
      return Container.stillValidBlockEntity(this, p_256481_);
   }

   public int getLastInteractedSlot() {
      return this.lastInteractedSlot;
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_393028_) {
      super.applyImplicitComponents(p_393028_);
      p_393028_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_331494_) {
      super.collectImplicitComponents(p_331494_);
      p_331494_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_405909_) {
      p_405909_.discard("Items");
   }
}
