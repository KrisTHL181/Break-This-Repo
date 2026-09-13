package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    public static final Identifier INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    protected static final int BACKGROUND_TEXTURE_WIDTH = 256;
    protected static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    protected static final int DEFAULT_IMAGE_WIDTH = 176;
    protected static final int DEFAULT_IMAGE_HEIGHT = 166;
    protected final int imageWidth;
    protected final int imageHeight;
    protected int titleLabelX;
    protected int titleLabelY;
    protected int inventoryLabelX;
    protected int inventoryLabelY;
    private final List<ItemSlotMouseAction> itemSlotMouseActions;
    protected final T menu;
    protected final Component playerInventoryTitle;
    protected @Nullable Slot hoveredSlot;
    private @Nullable Slot lastClickSlot;
    protected int leftPos;
    protected int topPos;
    protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
    protected boolean isQuickCrafting;
    private int quickCraftingType;
    private @MouseButtonInfo.MouseButton int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private boolean doubleclick;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    public AbstractContainerScreen(final T menu, final Inventory inventory, final Component title) {
        this(menu, inventory, title, 176, 166);
    }

    public AbstractContainerScreen(final T menu, final Inventory inventory, final Component title, final int imageWidth, final int imageHeight) {
        super(title);
        this.menu = menu;
        this.playerInventoryTitle = inventory.getDisplayName();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.skipNextRelease = true;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = imageHeight - 94;
        this.itemSlotMouseActions = new ArrayList<>();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.itemSlotMouseActions.clear();
        this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));
    }

    protected void addItemSlotMouseAction(final ItemSlotMouseAction itemSlotMouseAction) {
        this.itemSlotMouseActions.add(itemSlotMouseAction);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        this.extractContents(graphics, mouseX, mouseY, a);
        this.extractCarriedItem(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        int xo = this.leftPos;
        int yo = this.topPos;
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.pose().pushMatrix();
        graphics.pose().translate(xo, yo);
        this.extractLabels(graphics, mouseX, mouseY);
        Slot previouslyHoveredSlot = this.hoveredSlot;
        this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
        this.extractSlotHighlightBack(graphics);
        this.extractSlots(graphics, mouseX, mouseY);
        this.extractSlotHighlightFront(graphics);
        if (previouslyHoveredSlot != null && previouslyHoveredSlot != this.hoveredSlot) {
            this.onStopHovering(previouslyHoveredSlot);
        }

        graphics.pose().popMatrix();
    }

    public void extractCarriedItem(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty()) {
            int xOffset = 8;
            int yOffset = 8;
            String itemCount = null;
            if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
                carried = carried.copyWithCount(this.quickCraftingRemainder);
                if (carried.isEmpty()) {
                    itemCount = ChatFormatting.YELLOW + "0";
                }
            }

            graphics.nextStratum();
            this.extractFloatingItem(graphics, carried, mouseX - 8, mouseY - 8, itemCount);
        }
    }

    protected void extractSlots(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                this.extractSlot(graphics, slot, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(this.hoveredSlot)
                    && itemMouseAction.onMouseScrolled(scrollX, scrollY, this.hoveredSlot.index, this.hoveredSlot.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void extractSlotHighlightBack(final GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    private void extractSlotHighlightFront(final GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack item = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty() || this.showTooltipWithItemInHand(item)) {
                graphics.setTooltipForNextFrame(
                    this.font, this.getTooltipFromContainerItem(item), item.getTooltipImage(), mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        }
    }

    private boolean showTooltipWithItemInHand(final ItemStack item) {
        return item.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false);
    }

    protected List<Component> getTooltipFromContainerItem(final ItemStack itemStack) {
        return getTooltipFromItem(this.minecraft, itemStack);
    }

    private void extractFloatingItem(final GuiGraphicsExtractor graphics, final ItemStack carried, final int x, final int y, final @Nullable String itemCount) {
        graphics.item(carried, x, y);
        graphics.itemDecorations(this.font, carried, x, y, itemCount);
    }

    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected void extractSlot(final GuiGraphicsExtractor graphics, final Slot slot, final int mouseX, final int mouseY) {
        int x = slot.x;
        int y = slot.y;
        ItemStack itemStack = slot.getItem();
        boolean quickCraftStack = false;
        boolean done = false;
        ItemStack carried = this.menu.getCarried();
        String itemCount = null;
        if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && this.menu.canDragTo(slot)) {
                quickCraftStack = true;
                int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int carry = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int newCount = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry;
                if (newCount > maxSize) {
                    newCount = maxSize;
                    itemCount = ChatFormatting.YELLOW.toString() + maxSize;
                }

                itemStack = carried.copyWithCount(newCount);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }

        if (itemStack.isEmpty() && slot.isActive()) {
            Identifier icon = slot.getNoItemIcon();
            if (icon != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16);
                done = true;
            }
        }

        if (!done) {
            if (quickCraftStack) {
                graphics.fill(x, y, x + 16, y + 16, -2130706433);
            }

            int seed = slot.x + slot.y * this.imageWidth;
            if (slot.isFake()) {
                graphics.fakeItem(itemStack, x, y, seed);
            } else {
                graphics.item(itemStack, x, y, seed);
            }

            graphics.itemDecorations(this.font, itemStack, x, y, itemCount);
        }
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty() && this.isQuickCrafting) {
            if (this.quickCraftingType == 2) {
                this.quickCraftingRemainder = carried.getMaxStackSize();
            } else {
                this.quickCraftingRemainder = carried.getCount();

                for (Slot slot : this.quickCraftSlots) {
                    ItemStack slotItemStack = slot.getItem();
                    int carry = slotItemStack.isEmpty() ? 0 : slotItemStack.getCount();
                    int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                    int newCount = Math.min(
                        AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry, maxSize
                    );
                    this.quickCraftingRemainder -= newCount - carry;
                }
            }
        }
    }

    private @Nullable Slot getHoveredSlot(final double x, final double y) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot, x, y)) {
                return slot;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        boolean cloning = this.minecraft.options.keyPickItem.matchesMouse(event) && this.minecraft.player.hasInfiniteMaterials();
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        this.doubleclick = this.lastClickSlot == slot && doubleClick;
        this.skipNextRelease = false;
        if (event.button() != 0 && event.button() != 1 && !cloning) {
            this.checkHotbarMouseClicked(event);
        } else {
            int xo = this.leftPos;
            int yo = this.topPos;
            boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
            int slotId = -1;
            if (slot != null) {
                slotId = slot.index;
            }

            if (clickedOutside) {
                slotId = -999;
            }

            if (slotId != -1 && !this.isQuickCrafting) {
                if (this.menu.getCarried().isEmpty()) {
                    if (cloning) {
                        this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                    } else {
                        boolean quickKey = slotId != -999 && event.hasShiftDown();
                        ContainerInput containerInput = ContainerInput.PICKUP;
                        if (quickKey) {
                            this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                            containerInput = ContainerInput.QUICK_MOVE;
                        } else if (slotId == -999) {
                            containerInput = ContainerInput.THROW;
                        }

                        this.slotClicked(slot, slotId, event.button(), containerInput);
                    }

                    this.skipNextRelease = true;
                } else {
                    this.isQuickCrafting = true;
                    this.quickCraftingButton = event.button();
                    this.quickCraftSlots.clear();
                    if (event.button() == 0) {
                        this.quickCraftingType = 0;
                    } else if (event.button() == 1) {
                        this.quickCraftingType = 1;
                    } else if (cloning) {
                        this.quickCraftingType = 2;
                    }
                }
            }
        }

        this.lastClickSlot = slot;
        return true;
    }

    private void checkHotbarMouseClicked(final MouseButtonEvent event) {
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.matchesMouse(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                }
            }
        }
    }

    protected boolean hasClickedOutside(final double mx, final double my, final int xo, final int yo) {
        return mx < xo || my < yo || mx >= xo + this.imageWidth || my >= yo + this.imageHeight;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        ItemStack carried = this.menu.getCarried();
        if (slot != null && this.shouldAddSlotToQuickCraft(slot, carried) && this.quickCraftSlots.add(slot)) {
            this.recalculateQuickCraftRemaining();
            return true;
        } else {
            return slot == null && this.menu.getCarried().isEmpty() ? super.mouseDragged(event, dx, dy) : true;
        }
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        int xo = this.leftPos;
        int yo = this.topPos;
        boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
        int slotId = -1;
        if (slot != null) {
            slotId = slot.index;
        }

        if (clickedOutside) {
            slotId = -999;
        }

        if (this.doubleclick && slot != null && event.button() == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (event.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot target : this.menu.slots) {
                        if (target != null
                            && target.mayPickup(this.minecraft.player)
                            && target.hasItem()
                            && target.container == slot.container
                            && AbstractContainerMenu.canItemQuickReplace(target, this.lastQuickMoved, true)) {
                            this.slotClicked(target, target.index, event.button(), ContainerInput.QUICK_MOVE);
                        }
                    }
                }
            } else {
                this.slotClicked(slot, slotId, event.button(), ContainerInput.PICKUP_ALL);
            }

            this.doubleclick = false;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != event.button()) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                return true;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }

            if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.quickCraftToSlots();
            } else if (!this.menu.getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.matchesMouse(event)) {
                    this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                } else {
                    boolean quickKey = slotId != -999 && event.hasShiftDown();
                    if (quickKey) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }

                    this.slotClicked(slot, slotId, event.button(), quickKey ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                }
            }
        }

        this.isQuickCrafting = false;
        return super.mouseReleased(event);
    }

    private boolean isHovering(final Slot slot, final double xm, final double ym) {
        return this.isHovering(slot.x, slot.y, 16, 16, xm, ym);
    }

    protected boolean isHovering(final int left, final int top, final int w, final int h, double xm, double ym) {
        int xo = this.leftPos;
        int yo = this.topPos;
        xm -= xo;
        ym -= yo;
        return xm >= left - 1 && xm < left + w + 1 && ym >= top - 1 && ym < top + h + 1;
    }

    private void onStopHovering(final Slot slot) {
        if (slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(slot)) {
                    itemMouseAction.onStopHovering(slot);
                }
            }
        }
    }

    protected void slotClicked(final Slot slot, int slotId, final int buttonNum, final ContainerInput containerInput) {
        if (slot != null) {
            slotId = slot.index;
        }

        this.onMouseClickAction(slot, containerInput);
        this.minecraft.gameMode.handleContainerInput(this.menu.containerId, slotId, buttonNum, containerInput, this.minecraft.player);
    }

    protected void onMouseClickAction(final @Nullable Slot slot, final ContainerInput containerInput) {
        if (slot != null && slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (itemMouseAction.matches(slot)) {
                    itemMouseAction.onSlotClicked(slot, containerInput);
                }
            }
        }
    }

    protected void handleSlotStateChanged(final int slotId, final int containerId, final boolean newState) {
        this.minecraft.gameMode.handleSlotStateChanged(slotId, containerId, newState);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }

        if (this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        }

        this.checkHotbarKeyPressed(event);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if (this.minecraft.options.keyPickItem.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
            } else if (this.minecraft.options.keyDrop.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
            }
        }

        return false;
    }

    protected boolean checkHotbarKeyPressed(final KeyEvent event) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.minecraft.options.keySwapOffhand.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return true;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void removed() {
        if (this.minecraft.player != null) {
            this.menu.removed(this.minecraft.player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public final void tick() {
        super.tick();
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }
    }

    protected void containerTick() {
    }

    private boolean shouldAddSlotToQuickCraft(final Slot slot, final ItemStack carried) {
        return this.isQuickCrafting
            && !carried.isEmpty()
            && (carried.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
            && AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
            && slot.mayPlace(carried)
            && this.menu.canDragTo(slot);
    }

    private void quickCraftToSlots() {
        this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ContainerInput.QUICK_CRAFT);

        for (Slot quickSlot : this.quickCraftSlots) {
            this.slotClicked(quickSlot, quickSlot.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
        }

        this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
    }

    @Override
    public T getMenu() {
        return this.menu;
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        if (this.hoveredSlot != null) {
            this.onStopHovering(this.hoveredSlot);
        }

        super.onClose();
    }
}