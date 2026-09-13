package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CreativeModeInventoryScreen extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final Identifier[] UNSELECTED_TOP_TABS = new Identifier[]{
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_3"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_4"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_5"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_6"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_7")
    };
    private static final Identifier[] SELECTED_TOP_TABS = new Identifier[]{
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_2"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_3"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_4"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_5"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_6"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_7")
    };
    private static final Identifier[] UNSELECTED_BOTTOM_TABS = new Identifier[]{
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_1"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_2"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_3"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_4"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_5"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_6"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_7")
    };
    private static final Identifier[] SELECTED_BOTTOM_TABS = new Identifier[]{
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_1"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_2"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_3"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_4"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_5"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_6"),
        Identifier.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_7")
    };
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 9;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final SimpleContainer CONTAINER = new SimpleContainer(45);
    private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");
    private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
    private float scrollOffs;
    private boolean scrolling;
    private EditBox searchBox;
    private @Nullable List<Slot> originalSlots;
    private @Nullable Slot destroyItemSlot;
    private CreativeInventoryListener listener;
    private boolean ignoreTextInput;
    private boolean hasClickedOutside;
    private final Set<TagKey<Item>> visibleTags = new HashSet<>();
    private final boolean displayOperatorCreativeTab;
    private final EffectsInInventory effects;

    public CreativeModeInventoryScreen(final LocalPlayer player, final FeatureFlagSet enabledFeatures, final boolean displayOperatorCreativeTab) {
        super(new CreativeModeInventoryScreen.ItemPickerMenu(player), player.getInventory(), CommonComponents.EMPTY, 195, 136);
        player.containerMenu = this.menu;
        this.displayOperatorCreativeTab = displayOperatorCreativeTab;
        this.tryRebuildTabContents(player.connection.searchTrees(), enabledFeatures, this.hasPermissions(player), player.level().registryAccess());
        this.effects = new EffectsInInventory(this);
    }

    private boolean hasPermissions(final Player player) {
        return player.canUseGameMasterBlocks() && this.displayOperatorCreativeTab;
    }

    private void tryRefreshInvalidatedTabs(final FeatureFlagSet enabledFeatures, final boolean hasPermissions, final HolderLookup.Provider holders) {
        ClientPacketListener connection = this.minecraft.getConnection();
        if (this.tryRebuildTabContents(connection != null ? connection.searchTrees() : null, enabledFeatures, hasPermissions, holders)) {
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                Collection<ItemStack> displayList = tab.getDisplayItems();
                if (tab == selectedTab) {
                    if (tab.getType() == CreativeModeTab.Type.CATEGORY && displayList.isEmpty()) {
                        this.selectTab(CreativeModeTabs.getDefaultTab());
                    } else {
                        this.refreshCurrentTabContents(displayList);
                    }
                }
            }
        }
    }

    private boolean tryRebuildTabContents(
        final @Nullable SessionSearchTrees searchTrees, final FeatureFlagSet enabledFeatures, final boolean hasPermissions, final HolderLookup.Provider holders
    ) {
        if (!CreativeModeTabs.tryRebuildTabContents(enabledFeatures, hasPermissions, holders)) {
            return false;
        }

        if (searchTrees != null) {
            List<ItemStack> creativeSearchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
            searchTrees.updateCreativeTooltips(holders, creativeSearchItems);
            searchTrees.updateCreativeTags(creativeSearchItems);
        }

        return true;
    }

    private void refreshCurrentTabContents(final Collection<ItemStack> displayList) {
        int oldRowIndex = this.menu.getRowIndexForScroll(this.scrollOffs);
        this.menu.items.clear();
        if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
            this.refreshSearchResults();
        } else {
            this.menu.items.addAll(displayList);
        }

        this.scrollOffs = this.menu.getScrollForRowIndex(oldRowIndex);
        this.menu.scrollTo(this.scrollOffs);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            this.tryRefreshInvalidatedTabs(player.connection.enabledFeatures(), this.hasPermissions(player), player.level().registryAccess());
            if (!player.hasInfiniteMaterials()) {
                this.minecraft.gui.setScreen(new InventoryScreen(player));
            }
        }
    }

    @Override
    protected void slotClicked(final @Nullable Slot slot, final int slotId, final int buttonNum, ContainerInput containerInput) {
        if (this.isCreativeSlot(slot)) {
            this.searchBox.moveCursorToEnd(false);
            this.searchBox.setHighlightPos(0);
        }

        boolean quickKey = containerInput == ContainerInput.QUICK_MOVE;
        containerInput = slotId == -999 && containerInput == ContainerInput.PICKUP ? ContainerInput.THROW : containerInput;
        if (containerInput != ContainerInput.THROW || this.minecraft.player.canDropItems()) {
            this.onMouseClickAction(slot, containerInput);
            if (slot == null && selectedTab.getType() != CreativeModeTab.Type.INVENTORY && containerInput != ContainerInput.QUICK_CRAFT) {
                if (!this.menu.getCarried().isEmpty() && this.hasClickedOutside) {
                    if (!this.minecraft.player.canDropItems()) {
                        return;
                    }

                    if (buttonNum == 0) {
                        this.minecraft.player.drop(this.menu.getCarried(), true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
                        this.menu.setCarried(ItemStack.EMPTY);
                    }

                    if (buttonNum == 1) {
                        ItemStack removedItem = this.menu.getCarried().split(1);
                        this.minecraft.player.drop(removedItem, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(removedItem);
                    }
                }
            } else {
                if (slot != null && !slot.mayPickup(this.minecraft.player)) {
                    return;
                }

                if (slot == this.destroyItemSlot && quickKey) {
                    for (int i = 0; i < this.minecraft.player.inventoryMenu.getItems().size(); i++) {
                        this.minecraft.player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
                        this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, i);
                    }
                } else if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
                    if (slot == this.destroyItemSlot) {
                        this.menu.setCarried(ItemStack.EMPTY);
                    } else if (containerInput == ContainerInput.THROW && slot != null && slot.hasItem()) {
                        ItemStack toDrop = slot.remove(buttonNum == 0 ? 1 : slot.getItem().getMaxStackSize());
                        ItemStack afterDrop = slot.getItem();
                        this.minecraft.player.drop(toDrop, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(toDrop);
                        this.minecraft.gameMode.handleCreativeModeItemAdd(afterDrop, ((CreativeModeInventoryScreen.SlotWrapper)slot).target.index);
                    } else if (containerInput == ContainerInput.THROW && slotId == -999 && !this.menu.getCarried().isEmpty()) {
                        this.minecraft.player.drop(this.menu.getCarried(), true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
                        this.menu.setCarried(ItemStack.EMPTY);
                    } else {
                        this.minecraft
                            .player
                            .inventoryMenu
                            .clicked(
                                slot == null ? slotId : ((CreativeModeInventoryScreen.SlotWrapper)slot).target.index,
                                buttonNum,
                                containerInput,
                                this.minecraft.player
                            );
                        this.minecraft.player.inventoryMenu.broadcastChanges();
                    }
                } else if (containerInput != ContainerInput.QUICK_CRAFT && slot.container == CONTAINER) {
                    ItemStack carried = this.menu.getCarried();
                    ItemStack clicked = slot.getItem();
                    if (containerInput == ContainerInput.SWAP) {
                        if (!clicked.isEmpty()) {
                            this.minecraft.player.getInventory().setItem(buttonNum, clicked.copyWithCount(clicked.getMaxStackSize()));
                            this.minecraft.player.inventoryMenu.broadcastChanges();
                        }

                        return;
                    }

                    if (containerInput == ContainerInput.CLONE) {
                        if (this.menu.getCarried().isEmpty() && slot.hasItem()) {
                            ItemStack item = slot.getItem();
                            this.menu.setCarried(item.copyWithCount(item.getMaxStackSize()));
                        }

                        return;
                    }

                    if (containerInput == ContainerInput.THROW) {
                        if (!clicked.isEmpty()) {
                            ItemStack toDrop = clicked.copyWithCount(buttonNum == 0 ? 1 : clicked.getMaxStackSize());
                            this.minecraft.player.drop(toDrop, true);
                            this.minecraft.gameMode.handleCreativeModeItemDrop(toDrop);
                        }

                        return;
                    }

                    if (!carried.isEmpty() && !clicked.isEmpty() && ItemStack.isSameItemSameComponents(carried, clicked)) {
                        if (buttonNum == 0) {
                            if (quickKey) {
                                carried.setCount(carried.getMaxStackSize());
                            } else if (carried.getCount() < carried.getMaxStackSize()) {
                                carried.grow(1);
                            }
                        } else {
                            carried.shrink(1);
                        }
                    } else if (!clicked.isEmpty() && carried.isEmpty()) {
                        int count = quickKey ? clicked.getMaxStackSize() : clicked.getCount();
                        this.menu.setCarried(clicked.copyWithCount(count));
                    } else if (buttonNum == 0) {
                        this.menu.setCarried(ItemStack.EMPTY);
                    } else if (!this.menu.getCarried().isEmpty()) {
                        this.menu.getCarried().shrink(1);
                    }
                } else if (this.menu != null) {
                    ItemStack oldItemStack = slot == null ? ItemStack.EMPTY : this.menu.getSlot(slot.index).getItem();
                    this.menu.clicked(slot == null ? slotId : slot.index, buttonNum, containerInput, this.minecraft.player);
                    if (AbstractContainerMenu.getQuickcraftHeader(buttonNum) == 2) {
                        for (int i = 0; i < 9; i++) {
                            this.minecraft.gameMode.handleCreativeModeItemAdd(this.menu.getSlot(45 + i).getItem(), 36 + i);
                        }
                    } else if (slot != null && Inventory.isHotbarSlot(slot.getContainerSlot()) && selectedTab.getType() != CreativeModeTab.Type.INVENTORY) {
                        if (containerInput == ContainerInput.THROW && !oldItemStack.isEmpty() && !this.menu.getCarried().isEmpty()) {
                            int numToDrop = buttonNum == 0 ? 1 : oldItemStack.getCount();
                            ItemStack toDrop = oldItemStack.copyWithCount(numToDrop);
                            oldItemStack.shrink(numToDrop);
                            this.minecraft.player.drop(toDrop, true);
                            this.minecraft.gameMode.handleCreativeModeItemDrop(toDrop);
                        }

                        this.minecraft.player.inventoryMenu.broadcastChanges();
                    }
                }
            }
        }
    }

    private boolean isCreativeSlot(final @Nullable Slot slot) {
        return slot != null && slot.container == CONTAINER;
    }

    @Override
    protected void init() {
        if (this.minecraft.player.hasInfiniteMaterials()) {
            super.init();
            this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
            this.searchBox.setMaxLength(50);
            this.searchBox.setBordered(false);
            this.searchBox.setVisible(false);
            this.searchBox.setTextColor(-1);
            this.searchBox.setInvertHighlightedTextColor(false);
            this.addWidget(this.searchBox);
            CreativeModeTab tab = selectedTab;
            selectedTab = CreativeModeTabs.getDefaultTab();
            this.selectTab(tab);
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
            this.listener = new CreativeInventoryListener(this.minecraft);
            this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
            if (!selectedTab.shouldDisplay()) {
                this.selectTab(CreativeModeTabs.getDefaultTab());
            }
        } else {
            this.minecraft.gui.setScreen(new InventoryScreen(this.minecraft.player));
        }
    }

    @Override
    public void resize(final int width, final int height) {
        int oldRowIndex = this.menu.getRowIndexForScroll(this.scrollOffs);
        String oldEdit = this.searchBox.getValue();
        this.init(width, height);
        this.searchBox.setValue(oldEdit);
        if (!this.searchBox.getValue().isEmpty()) {
            this.refreshSearchResults();
        }

        this.scrollOffs = this.menu.getScrollForRowIndex(oldRowIndex);
        this.menu.scrollTo(this.scrollOffs);
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        }
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.ignoreTextInput) {
            return false;
        }

        if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
            return false;
        }

        String oldContents = this.searchBox.getValue();
        if (this.searchBox.charTyped(event)) {
            if (!Objects.equals(oldContents, this.searchBox.getValue())) {
                this.refreshSearchResults();
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean preeditUpdated(final @Nullable PreeditEvent event) {
        if (this.ignoreTextInput) {
            return false;
        } else {
            return selectedTab.getType() != CreativeModeTab.Type.SEARCH ? false : this.searchBox.preeditUpdated(event);
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        this.ignoreTextInput = false;
        if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
            if (this.minecraft.options.keyChat.matches(event)) {
                this.ignoreTextInput = true;
                this.selectTab(CreativeModeTabs.searchTab());
                return true;
            } else {
                return super.keyPressed(event);
            }
        } else {
            boolean doQuickSwap = !this.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
            boolean pressingNumber = InputConstants.getKey(event).getNumericKeyValue().isPresent();
            if (doQuickSwap && pressingNumber && this.checkHotbarKeyPressed(event)) {
                this.ignoreTextInput = true;
                return true;
            }

            String oldContents = this.searchBox.getValue();
            if (this.searchBox.keyPressed(event)) {
                if (!Objects.equals(oldContents, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }

                return true;
            } else {
                return this.searchBox.isFocused() && this.searchBox.isVisible() && !event.isEscape() ? true : super.keyPressed(event);
            }
        }
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        this.ignoreTextInput = false;
        return super.keyReleased(event);
    }

    private void refreshSearchResults() {
        this.menu.items.clear();
        this.visibleTags.clear();
        String searchTerm = this.searchBox.getValue();
        if (searchTerm.isEmpty()) {
            this.menu.items.addAll(selectedTab.getDisplayItems());
        } else {
            ClientPacketListener connection = this.minecraft.getConnection();
            if (connection != null) {
                SessionSearchTrees searchTrees = connection.searchTrees();
                SearchTree<ItemStack> tree;
                if (searchTerm.startsWith("#")) {
                    searchTerm = searchTerm.substring(1);
                    tree = searchTrees.creativeTagSearch();
                    this.updateVisibleTags(searchTerm);
                } else {
                    tree = searchTrees.creativeNameSearch();
                }

                this.menu.items.addAll(tree.search(searchTerm.toLowerCase(Locale.ROOT)));
            }
        }

        this.scrollOffs = 0.0F;
        this.menu.scrollTo(0.0F);
    }

    private void updateVisibleTags(final String searchTerm) {
        int colonIndex = searchTerm.indexOf(58);
        Predicate<Identifier> matcher;
        if (colonIndex == -1) {
            matcher = id -> id.getPath().contains(searchTerm);
        } else {
            String nsMatcher = searchTerm.substring(0, colonIndex).trim();
            String pathMatcher = searchTerm.substring(colonIndex + 1).trim();
            matcher = id -> id.getNamespace().contains(nsMatcher) && id.getPath().contains(pathMatcher);
        }

        BuiltInRegistries.ITEM.getTags().map(HolderSet.Named::key).filter(tag -> matcher.test(tag.location())).forEach(this.visibleTags::add);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        if (selectedTab.showTitle()) {
            graphics.text(this.font, selectedTab.getDisplayName(), 8, 6, -12566464, false);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() == 0) {
            double xm = event.x() - this.leftPos;
            double ym = event.y() - this.topPos;

            for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
                if (this.checkTabClicked(tab, xm, ym)) {
                    return true;
                }
            }

            if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY && this.insideScrollbar(event.x(), event.y())) {
                this.scrolling = this.canScroll();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (event.button() == 0) {
            double xm = event.x() - this.leftPos;
            double ym = event.y() - this.topPos;
            this.scrolling = false;

            for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
                if (this.checkTabClicked(tab, xm, ym)) {
                    this.selectTab(tab);
                    return true;
                }
            }
        }

        return super.mouseReleased(event);
    }

    private boolean canScroll() {
        return selectedTab.canScroll() && this.menu.canScroll();
    }

    private void selectTab(final CreativeModeTab tab) {
        CreativeModeTab oldTab = selectedTab;
        selectedTab = tab;
        this.quickCraftSlots.clear();
        this.menu.items.clear();
        if (selectedTab.getType() == CreativeModeTab.Type.HOTBAR) {
            HotbarManager manager = this.minecraft.getHotbarManager();

            for (int hotbarIndex = 0; hotbarIndex < 9; hotbarIndex++) {
                Hotbar hotbar = manager.get(hotbarIndex);
                if (hotbar.isEmpty()) {
                    for (int i = 0; i < 9; i++) {
                        if (i == hotbarIndex) {
                            ItemStack placeholder = new ItemStack(Items.PAPER);
                            placeholder.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                            Component translatedKeyMessage = this.minecraft.options.keyHotbarSlots[hotbarIndex].getTranslatedKeyMessage();
                            Component activatorKeyMessage = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                            placeholder.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo", activatorKeyMessage, translatedKeyMessage));
                            this.menu.items.add(placeholder);
                        } else {
                            this.menu.items.add(ItemStack.EMPTY);
                        }
                    }
                } else {
                    this.menu.items.addAll(hotbar.load(this.minecraft.level.registryAccess()));
                }
            }
        } else if (selectedTab.getType() == CreativeModeTab.Type.CATEGORY) {
            this.menu.items.addAll(selectedTab.getDisplayItems());
        }

        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            AbstractContainerMenu invMenu = this.minecraft.player.inventoryMenu;
            if (this.originalSlots == null) {
                this.originalSlots = ImmutableList.copyOf(this.menu.slots);
            }

            this.menu.slots.clear();

            for (int i = 0; i < invMenu.slots.size(); i++) {
                int x;
                int y;
                if (i >= 5 && i < 9) {
                    int pos = i - 5;
                    int col = pos / 2;
                    int row = pos % 2;
                    x = 54 + col * 54;
                    y = 6 + row * 27;
                } else if (i >= 0 && i < 5) {
                    x = -2000;
                    y = -2000;
                } else if (i == 45) {
                    x = 35;
                    y = 20;
                } else {
                    int pos = i - 9;
                    int col = pos % 9;
                    int row = pos / 9;
                    x = 9 + col * 18;
                    if (i >= 36) {
                        y = 112;
                    } else {
                        y = 54 + row * 18;
                    }
                }

                Slot slot = new CreativeModeInventoryScreen.SlotWrapper(invMenu.slots.get(i), i, x, y);
                this.menu.slots.add(slot);
            }

            this.destroyItemSlot = new Slot(CONTAINER, 0, 173, 112);
            this.menu.slots.add(this.destroyItemSlot);
        } else if (oldTab.getType() == CreativeModeTab.Type.INVENTORY) {
            this.menu.slots.clear();
            this.menu.slots.addAll(this.originalSlots);
            this.originalSlots = null;
        }

        if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
            this.searchBox.setVisible(true);
            this.searchBox.setCanLoseFocus(false);
            this.searchBox.setFocused(true);
            if (oldTab != tab) {
                this.searchBox.setValue("");
            }

            this.refreshSearchResults();
        } else {
            this.searchBox.setVisible(false);
            this.searchBox.setCanLoseFocus(true);
            this.searchBox.setFocused(false);
            this.searchBox.setValue("");
        }

        this.scrollOffs = 0.0F;
        this.menu.scrollTo(0.0F);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }

        if (!this.canScroll()) {
            return false;
        }

        this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs, scrollY);
        this.menu.scrollTo(this.scrollOffs);
        return true;
    }

    @Override
    protected boolean hasClickedOutside(final double mx, final double my, final int xo, final int yo) {
        boolean clickedOutside = mx < xo || my < yo || mx >= xo + this.imageWidth || my >= yo + this.imageHeight;
        this.hasClickedOutside = clickedOutside && !this.checkTabClicked(selectedTab, mx, my);
        return this.hasClickedOutside;
    }

    protected boolean insideScrollbar(final double xm, final double ym) {
        int xo = this.leftPos;
        int yo = this.topPos;
        int xscr = xo + 175;
        int yscr = yo + 18;
        int xscr2 = xscr + 14;
        int yscr2 = yscr + 112;
        return xm >= xscr && ym >= yscr && xm < xscr2 && ym < yscr2 && selectedTab.canScroll();
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        if (this.scrolling) {
            int yscr = this.topPos + 18;
            int yscr2 = yscr + 112;
            this.scrollOffs = ((float)event.y() - yscr - 7.5F) / (yscr2 - yscr - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.menu.scrollTo(this.scrollOffs);
            return true;
        } else {
            return super.mouseDragged(event, dx, dy);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        this.effects.extractRenderState(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, a);

        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (this.checkTabHovering(graphics, tab, mouseX, mouseY)) {
                break;
            }
        }

        if (this.destroyItemSlot != null
            && selectedTab.getType() == CreativeModeTab.Type.INVENTORY
            && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, TRASH_SLOT_TOOLTIP, mouseX, mouseY);
        }
    }

    @Override
    public boolean showsActiveEffects() {
        return this.effects.canSeeEffects();
    }

    @Override
    public List<Component> getTooltipFromContainerItem(final ItemStack itemStack) {
        boolean isCreativeSlot = this.hoveredSlot != null && this.hoveredSlot instanceof CreativeModeInventoryScreen.CustomCreativeSlot;
        boolean isSingleCategoryTab = selectedTab.getType() == CreativeModeTab.Type.CATEGORY;
        boolean isSearchTab = selectedTab.getType() == CreativeModeTab.Type.SEARCH;
        TooltipFlag.Default originalTooltipStyle = this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        TooltipFlag tooltipStyle = isCreativeSlot ? originalTooltipStyle.asCreative() : originalTooltipStyle;
        List<Component> originalLines = itemStack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, tooltipStyle);
        if (originalLines.isEmpty()) {
            return originalLines;
        }

        if (isSingleCategoryTab && isCreativeSlot) {
            return originalLines;
        }

        List<Component> linesToDisplay = Lists.newArrayList(originalLines);
        if (isSearchTab && isCreativeSlot) {
            this.visibleTags.forEach(tag -> {
                if (itemStack.is((TagKey<Item>)tag)) {
                    linesToDisplay.add(1, Component.literal("#" + tag.location()).withStyle(ChatFormatting.DARK_PURPLE));
                }
            });
        }

        int i = 1;

        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (tab.getType() != CreativeModeTab.Type.SEARCH && tab.contains(itemStack)) {
                linesToDisplay.add(i++, tab.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
            }
        }

        return linesToDisplay;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (tab != selectedTab) {
                this.extractTabButton(graphics, mouseX, mouseY, tab);
            }
        }

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            selectedTab.getBackgroundTexture(),
            this.leftPos,
            this.topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            256,
            256
        );
        if (this.insideScrollbar(mouseX, mouseY)) {
            if (this.canScroll()) {
                graphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            } else {
                graphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        }

        this.searchBox.extractRenderState(graphics, mouseX, mouseY, a);
        int xscr = this.leftPos + 175;
        int yscr = this.topPos + 18;
        int yscr2 = yscr + 112;
        if (selectedTab.canScroll()) {
            Identifier sprite = this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xscr, yscr + (int)((yscr2 - yscr - 17) * this.scrollOffs), 12, 15);
        }

        this.extractTabButton(graphics, mouseX, mouseY, selectedTab);
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics, this.leftPos + 73, this.topPos + 6, this.leftPos + 105, this.topPos + 49, 20, 0.0625F, mouseX, mouseY, this.minecraft.player
            );
        }
    }

    private int getTabX(final CreativeModeTab tab) {
        int pos = tab.column();
        int spacing = 27;
        int x = 27 * pos;
        if (tab.isAlignedRight()) {
            x = this.imageWidth - 27 * (7 - pos) + 1;
        }

        return x;
    }

    private int getTabY(final CreativeModeTab tab) {
        int y = 0;
        if (tab.row() == CreativeModeTab.Row.TOP) {
            y -= 32;
        } else {
            y += this.imageHeight;
        }

        return y;
    }

    protected boolean checkTabClicked(final CreativeModeTab tab, final double xm, final double ym) {
        int x = this.getTabX(tab);
        int y = this.getTabY(tab);
        return xm >= x && xm <= x + 26 && ym >= y && ym <= y + 32;
    }

    protected boolean checkTabHovering(final GuiGraphicsExtractor graphics, final CreativeModeTab tab, final int xm, final int ym) {
        int x = this.getTabX(tab);
        int y = this.getTabY(tab);
        if (this.isHovering(x + 3, y + 3, 21, 27, xm, ym)) {
            graphics.setTooltipForNextFrame(this.font, tab.getDisplayName(), xm, ym);
            return true;
        } else {
            return false;
        }
    }

    protected void extractTabButton(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final CreativeModeTab tab) {
        boolean selected = tab == selectedTab;
        boolean isTop = tab.row() == CreativeModeTab.Row.TOP;
        int pos = tab.column();
        int x = this.leftPos + this.getTabX(tab);
        int y = this.topPos - (isTop ? 28 : -(this.imageHeight - 4));
        Identifier[] sprites;
        if (isTop) {
            sprites = selected ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS;
        } else {
            sprites = selected ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS;
        }

        if (!selected && mouseX > x && mouseY > y && mouseX < x + 26 && mouseY < y + 32) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites[Mth.clamp(pos, 0, sprites.length)], x, y, 26, 32);
        int iconX = x + 13 - 8;
        int iconY = y + 16 - 8 + (isTop ? 1 : -1);
        graphics.item(tab.getIconItem(), iconX, iconY);
    }

    public boolean isInventoryOpen() {
        return selectedTab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    public static void handleHotbarLoadOrSave(final Minecraft minecraft, final int index, final boolean isLoadPressed, final boolean isSavePressed) {
        LocalPlayer player = minecraft.player;
        RegistryAccess registries = player.level().registryAccess();
        HotbarManager manager = minecraft.getHotbarManager();
        Hotbar hotbar = manager.get(index);
        if (isLoadPressed) {
            List<ItemStack> originalItems = hotbar.load(registries);

            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                ItemStack itemStack = originalItems.get(i);
                player.getInventory().setItem(i, itemStack);
                minecraft.gameMode.handleCreativeModeItemAdd(itemStack, 36 + i);
            }

            player.inventoryMenu.broadcastChanges();
        } else if (isSavePressed) {
            hotbar.storeFrom(player.getInventory(), registries);
            Component translatedKeyMessage = minecraft.options.keyHotbarSlots[index].getTranslatedKeyMessage();
            Component activatorKeyMessage = minecraft.options.keyLoadHotbarActivator.getTranslatedKeyMessage();
            Component message = Component.translatable("inventory.hotbarSaved", activatorKeyMessage, translatedKeyMessage);
            minecraft.gui.hud.setOverlayMessage(message, false);
            minecraft.getNarrator().saySystemNow(message);
            manager.save();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class CustomCreativeSlot extends Slot {
        public CustomCreativeSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(final Player player) {
            ItemStack item = this.getItem();
            return super.mayPickup(player) && !item.isEmpty()
                ? item.isItemEnabled(player.level().enabledFeatures()) && !item.has(DataComponents.CREATIVE_SLOT_LOCK)
                : item.isEmpty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ItemPickerMenu extends AbstractContainerMenu {
        public final NonNullList<ItemStack> items = NonNullList.create();
        private final AbstractContainerMenu inventoryMenu;

        public ItemPickerMenu(final Player player) {
            super(null, 0);
            this.inventoryMenu = player.inventoryMenu;
            Inventory inventory = player.getInventory();

            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 9; x++) {
                    this.addSlot(new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER, y * 9 + x, 9 + x * 18, 18 + y * 18));
                }
            }

            this.addInventoryHotbarSlots(inventory, 9, 112);
            this.scrollTo(0.0F);
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }

        protected int calculateRowCount() {
            return Mth.positiveCeilDiv(this.items.size(), 9) - 5;
        }

        protected int getRowIndexForScroll(final float scrollOffs) {
            return Math.max((int)(scrollOffs * this.calculateRowCount() + 0.5), 0);
        }

        protected float getScrollForRowIndex(final int rowIndex) {
            return Mth.clamp((float)rowIndex / this.calculateRowCount(), 0.0F, 1.0F);
        }

        protected float subtractInputFromScroll(final float scrollOffs, final double input) {
            return Mth.clamp(scrollOffs - (float)(input / this.calculateRowCount()), 0.0F, 1.0F);
        }

        public void scrollTo(final float scrollOffs) {
            int rowToScrollTo = this.getRowIndexForScroll(scrollOffs);

            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 9; x++) {
                    int slot = x + (y + rowToScrollTo) * 9;
                    if (slot >= 0 && slot < this.items.size()) {
                        CreativeModeInventoryScreen.CONTAINER.setItem(x + y * 9, this.items.get(slot));
                    } else {
                        CreativeModeInventoryScreen.CONTAINER.setItem(x + y * 9, ItemStack.EMPTY);
                    }
                }
            }
        }

        public boolean canScroll() {
            return this.items.size() > 45;
        }

        @Override
        public ItemStack quickMoveStack(final Player player, final int slotIndex) {
            if (slotIndex >= this.slots.size() - 9 && slotIndex < this.slots.size()) {
                Slot slot = this.slots.get(slotIndex);
                if (slot != null && slot.hasItem()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                }
            }

            return ItemStack.EMPTY;
        }

        @Override
        public boolean canTakeItemForPickAll(final ItemStack carried, final Slot target) {
            return target.container != CreativeModeInventoryScreen.CONTAINER;
        }

        @Override
        public boolean canDragTo(final Slot slot) {
            return slot.container != CreativeModeInventoryScreen.CONTAINER;
        }

        @Override
        public ItemStack getCarried() {
            return this.inventoryMenu.getCarried();
        }

        @Override
        public void setCarried(final ItemStack carried) {
            this.inventoryMenu.setCarried(carried);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class SlotWrapper extends Slot {
        private final Slot target;

        public SlotWrapper(final Slot target, final int index, final int x, final int y) {
            super(target.container, index, x, y);
            this.target = target;
        }

        @Override
        public void onTake(final Player player, final ItemStack carried) {
            this.target.onTake(player, carried);
        }

        @Override
        public boolean mayPlace(final ItemStack itemStack) {
            return this.target.mayPlace(itemStack);
        }

        @Override
        public ItemStack getItem() {
            return this.target.getItem();
        }

        @Override
        public boolean hasItem() {
            return this.target.hasItem();
        }

        @Override
        public void setByPlayer(final ItemStack itemStack, final ItemStack previous) {
            this.target.setByPlayer(itemStack, previous);
        }

        @Override
        public void set(final ItemStack itemStack) {
            this.target.set(itemStack);
        }

        @Override
        public void setChanged() {
            this.target.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return this.target.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(final ItemStack itemStack) {
            return this.target.getMaxStackSize(itemStack);
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return this.target.getNoItemIcon();
        }

        @Override
        public ItemStack remove(final int amount) {
            return this.target.remove(amount);
        }

        @Override
        public boolean isActive() {
            return this.target.isActive();
        }

        @Override
        public boolean mayPickup(final Player player) {
            return this.target.mayPickup(player);
        }
    }
}