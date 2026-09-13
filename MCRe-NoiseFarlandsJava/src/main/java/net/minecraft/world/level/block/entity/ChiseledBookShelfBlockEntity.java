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

    public ChiseledBookShelfBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        super(BlockEntityTypes.CHISELED_BOOKSHELF, worldPosition, blockState);
    }

    private void updateState(final int interactedSlot) {
        if (interactedSlot >= 0 && interactedSlot < 6) {
            this.lastInteractedSlot = interactedSlot;
            BlockState updatedState = this.getBlockState();

            for (int slot = 0; slot < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); slot++) {
                boolean slotIsOccupied = !this.getItem(slot).isEmpty();
                BooleanProperty slotProperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot);
                updatedState = updatedState.setValue(slotProperty, slotIsOccupied);
            }

            Objects.requireNonNull(this.level).setBlock(this.worldPosition, updatedState, 3);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(updatedState));
        } else {
            LOGGER.error("Expected slot 0-5, got {}", interactedSlot);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        this.lastInteractedSlot = input.getIntOr("last_interacted_slot", -1);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items, true);
        output.putInt("last_interacted_slot", this.lastInteractedSlot);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean acceptsItemType(final ItemStack itemStack) {
        return itemStack.is(ItemTags.BOOKSHELF_BOOKS);
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        ItemStack retrievedItem = Objects.requireNonNullElse(this.getItems().get(slot), ItemStack.EMPTY);
        this.getItems().set(slot, ItemStack.EMPTY);
        if (!retrievedItem.isEmpty()) {
            this.updateState(slot);
        }

        return retrievedItem;
    }

    @Override
    public void setItem(final int slot, final ItemStack itemStack) {
        if (this.acceptsItemType(itemStack)) {
            this.getItems().set(slot, itemStack);
            this.updateState(slot);
        } else if (itemStack.isEmpty()) {
            this.removeItem(slot, this.getMaxStackSize());
        }
    }

    @Override
    public boolean canTakeItem(final Container into, final int slot, final ItemStack itemStack) {
        return into.hasAnyMatching(
            toItem -> toItem.isEmpty()
                ? true
                : ItemStack.isSameItemSameComponents(itemStack, toItem) && toItem.getCount() + itemStack.getCount() <= into.getMaxStackSize(toItem)
        );
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public boolean stillValid(final Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public int getLastInteractedSlot() {
        return this.lastInteractedSlot;
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
}