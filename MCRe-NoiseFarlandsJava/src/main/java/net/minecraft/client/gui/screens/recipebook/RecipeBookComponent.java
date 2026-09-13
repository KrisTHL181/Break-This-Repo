package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class RecipeBookComponent<T extends RecipeBookMenu> implements GuiEventListener, Renderable, NarratableEntry {
    public static final WidgetSprites RECIPE_BUTTON_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("recipe_book/button"), Identifier.withDefaultNamespace("recipe_book/button_highlighted")
    );
    protected static final Identifier RECIPE_BOOK_LOCATION = Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final int BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    private static final int OFFSET_X_POSITION = 86;
    private static final int BORDER_WIDTH = 8;
    private static final Component ALL_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.all");
    private static final int TICKS_TO_SWAP_SLOT = 30;
    private int xOffset;
    private int width;
    private int height;
    private float time;
    private @Nullable RecipeDisplayId lastPlacedRecipe;
    private final GhostSlots ghostSlots;
    private final List<RecipeBookTabButton> tabButtons = Lists.newArrayList();
    private @Nullable RecipeBookTabButton selectedTab;
    protected CycleButton<Boolean> filterButton;
    protected final T menu;
    protected Minecraft minecraft;
    private @Nullable EditBox searchBox;
    private String lastSearch = "";
    private final List<RecipeBookComponent.TabInfo> tabInfos;
    private ClientRecipeBook book;
    private final RecipeBookPage recipeBookPage;
    private @Nullable RecipeDisplayId lastRecipe;
    private @Nullable RecipeCollection lastRecipeCollection;
    private final StackedItemContents stackedContents = new StackedItemContents();
    private int timesInventoryChanged;
    private boolean ignoreTextInput;
    private boolean visible;
    private boolean widthTooNarrow;
    private @Nullable ScreenRectangle magnifierIconPlacement;

    public RecipeBookComponent(final T menu, final List<RecipeBookComponent.TabInfo> tabInfos) {
        this.menu = menu;
        this.tabInfos = tabInfos;
        SlotSelectTime slotSelectTime = () -> Mth.floor(this.time / 30.0F);
        this.ghostSlots = new GhostSlots(slotSelectTime);
        this.recipeBookPage = new RecipeBookPage(this, slotSelectTime, menu instanceof AbstractFurnaceMenu);
    }

    public void init(final int width, final int height, final Minecraft minecraft, final boolean widthTooNarrow) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        this.widthTooNarrow = widthTooNarrow;
        this.book = minecraft.player.getRecipeBook();
        this.timesInventoryChanged = minecraft.player.getInventory().getTimesChanged();
        this.visible = this.isVisibleAccordingToBookData();
        if (this.visible) {
            this.initVisuals();
        }
    }

    private void initVisuals() {
        boolean isFiltering = this.isFiltering();
        this.xOffset = this.widthTooNarrow ? 0 : 86;
        int xo = this.getXOrigin();
        int yo = this.getYOrigin();
        this.stackedContents.clear();
        this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
        this.menu.fillCraftSlotsStackedContents(this.stackedContents);
        String oldEdit = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.minecraft.font, xo + 25, yo + 13, 81, 9 + 5, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(-1);
        this.searchBox.setValue(oldEdit);
        this.searchBox.setHint(SEARCH_HINT);
        this.magnifierIconPlacement = ScreenRectangle.of(
            ScreenAxis.HORIZONTAL, xo + 8, this.searchBox.getY(), this.searchBox.getX() - this.getXOrigin(), this.searchBox.getHeight()
        );
        this.recipeBookPage.init(this.minecraft, xo, yo);
        this.filterButton = CycleButton.booleanBuilder(this.getRecipeFilterName(), ALL_RECIPES_TOOLTIP, isFiltering)
            .withTooltip(filtering -> filtering ? Tooltip.create(this.getRecipeFilterName()) : Tooltip.create(ALL_RECIPES_TOOLTIP))
            .withSprite((cycleButton, filtering) -> this.getFilterButtonTextures().get(filtering, cycleButton.isHoveredOrFocused()))
            .displayState(CycleButton.DisplayState.HIDE)
            .create(xo + 110, yo + 12, 26, 16, CommonComponents.EMPTY, (button, value) -> {
                this.toggleFiltering();
                this.sendUpdateSettings();
                this.updateCollections(false, value);
            });
        this.tabButtons.clear();

        for (RecipeBookComponent.TabInfo tabInfo : this.tabInfos) {
            this.tabButtons.add(new RecipeBookTabButton(0, 0, tabInfo, this::onTabButtonPress));
        }

        if (this.selectedTab != null) {
            this.selectedTab = this.tabButtons.stream().filter(o -> o.getCategory().equals(this.selectedTab.getCategory())).findFirst().orElse(null);
        }

        if (this.selectedTab == null) {
            this.selectedTab = this.tabButtons.get(0);
        }

        this.selectedTab.select();
        this.selectMatchingRecipes();
        this.updateTabs(isFiltering);
        this.updateCollections(false, isFiltering);
    }

    private int getYOrigin() {
        return (this.height - 166) / 2;
    }

    private int getXOrigin() {
        return (this.width - 147) / 2 - this.xOffset;
    }

    protected abstract WidgetSprites getFilterButtonTextures();

    public int updateScreenPosition(final int width, final int imageWidth) {
        int leftPos;
        if (this.isVisible() && !this.widthTooNarrow) {
            leftPos = 177 + (width - imageWidth - 200) / 2;
        } else {
            leftPos = (width - imageWidth) / 2;
        }

        return leftPos;
    }

    public void toggleVisibility() {
        this.setVisible(!this.isVisible());
    }

    public boolean isVisible() {
        return this.visible;
    }

    private boolean isVisibleAccordingToBookData() {
        return this.book.isOpen(this.menu.getRecipeBookType());
    }

    protected void setVisible(final boolean visible) {
        if (visible) {
            this.initVisuals();
        }

        this.visible = visible;
        this.book.setOpen(this.menu.getRecipeBookType(), visible);
        if (!visible) {
            this.recipeBookPage.setInvisible();
        }

        this.sendUpdateSettings();
    }

    protected abstract boolean isCraftingSlot(Slot slot);

    public void slotClicked(final @Nullable Slot slot) {
        if (slot != null && this.isCraftingSlot(slot)) {
            this.lastPlacedRecipe = null;
            this.ghostSlots.clear();
            if (this.isVisible()) {
                this.updateStackedContents();
            }
        }
    }

    private void selectMatchingRecipes() {
        for (RecipeBookComponent.TabInfo tabInfo : this.tabInfos) {
            for (RecipeCollection recipeCollection : this.book.getCollection(tabInfo.category())) {
                this.selectMatchingRecipes(recipeCollection, this.stackedContents);
            }
        }
    }

    protected abstract void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents);

    private void updateCollections(final boolean resetPage, final boolean isFiltering) {
        List<RecipeCollection> tabCollection = this.book.getCollection(this.selectedTab.getCategory());
        List<RecipeCollection> collection = Lists.newArrayList(tabCollection);
        collection.removeIf(c -> !c.hasAnySelected());
        String searchTarget = this.searchBox.getValue();
        if (!searchTarget.isEmpty()) {
            ClientPacketListener connection = this.minecraft.getConnection();
            if (connection != null) {
                ObjectSet<RecipeCollection> set = new ObjectLinkedOpenHashSet<>(
                    connection.searchTrees().recipes().search(searchTarget.toLowerCase(Locale.ROOT))
                );
                collection.removeIf(e -> !set.contains(e));
            }
        }

        if (isFiltering) {
            collection.removeIf(c -> !c.hasCraftable());
        }

        this.recipeBookPage.updateCollections(collection, resetPage, isFiltering);
    }

    private void updateTabs(final boolean isFiltering) {
        int xPosTab = (this.width - 147) / 2 - this.xOffset - 30;
        int yPosTab = (this.height - 166) / 2 + 3;
        int yOffset = 27;
        int index = 0;

        for (RecipeBookTabButton tabButton : this.tabButtons) {
            ExtendedRecipeBookCategory category = tabButton.getCategory();
            if (category instanceof SearchRecipeBookCategory) {
                tabButton.visible = true;
                tabButton.setPosition(xPosTab, yPosTab + 27 * index++);
            } else if (tabButton.updateVisibility(this.book)) {
                tabButton.setPosition(xPosTab, yPosTab + 27 * index++);
                tabButton.startAnimation(this.book, isFiltering);
            }
        }
    }

    public void tick() {
        boolean shouldBeVisible = this.isVisibleAccordingToBookData();
        if (this.isVisible() != shouldBeVisible) {
            this.setVisible(shouldBeVisible);
        }

        if (this.isVisible()) {
            if (this.timesInventoryChanged != this.minecraft.player.getInventory().getTimesChanged()) {
                this.updateStackedContents();
                this.timesInventoryChanged = this.minecraft.player.getInventory().getTimesChanged();
            }
        }
    }

    private void updateStackedContents() {
        this.stackedContents.clear();
        this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
        this.menu.fillCraftSlotsStackedContents(this.stackedContents);
        this.selectMatchingRecipes();
        this.updateCollections(false, this.isFiltering());
    }

    private boolean isFiltering() {
        return this.book.isFiltering(this.menu.getRecipeBookType());
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.isVisible()) {
            if (!this.minecraft.hasControlDown()) {
                this.time += a;
            }

            int xo = this.getXOrigin();
            int yo = this.getYOrigin();
            graphics.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_LOCATION, xo, yo, 1.0F, 1.0F, 147, 166, 256, 256);
            this.searchBox.extractRenderState(graphics, mouseX, mouseY, a);

            for (RecipeBookTabButton tabButton : this.tabButtons) {
                tabButton.extractRenderState(graphics, mouseX, mouseY, a);
            }

            this.filterButton.extractRenderState(graphics, mouseX, mouseY, a);
            this.recipeBookPage.extractRenderState(graphics, xo, yo, mouseX, mouseY, a);
        }
    }

    public void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final @Nullable Slot hoveredSlot) {
        if (this.isVisible()) {
            this.recipeBookPage.extractTooltip(graphics, mouseX, mouseY);
            this.ghostSlots.extractTooltip(graphics, this.minecraft, mouseX, mouseY, hoveredSlot);
        }
    }

    protected abstract Component getRecipeFilterName();

    public void extractGhostRecipe(final GuiGraphicsExtractor graphics, final boolean isResultSlotBig) {
        this.ghostSlots.extractRenderState(graphics, this.minecraft, isResultSlotBig);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.isVisible() && !this.minecraft.player.isSpectator()) {
            if (this.recipeBookPage.mouseClicked(event, this.getXOrigin(), this.getYOrigin(), 147, 166, doubleClick)) {
                RecipeDisplayId recipe = this.recipeBookPage.getLastClickedRecipe();
                RecipeCollection recipeCollection = this.recipeBookPage.getLastClickedRecipeCollection();
                if (recipe != null && recipeCollection != null) {
                    if (!this.tryPlaceRecipe(recipeCollection, recipe, event.hasShiftDown())) {
                        return false;
                    }

                    this.lastRecipeCollection = recipeCollection;
                    this.lastRecipe = recipe;
                    if (!this.isOffsetNextToMainGUI()) {
                        this.setVisible(false);
                    }
                }

                return true;
            } else {
                if (this.searchBox != null) {
                    boolean clickedMagnifierIcon = this.magnifierIconPlacement != null
                        && this.magnifierIconPlacement.containsPoint(Mth.floor(event.x()), Mth.floor(event.y()));
                    if (clickedMagnifierIcon || this.searchBox.mouseClicked(event, doubleClick)) {
                        this.searchBox.setFocused(true);
                        return true;
                    }

                    this.searchBox.setFocused(false);
                }

                if (this.filterButton.mouseClicked(event, doubleClick)) {
                    return true;
                }

                for (RecipeBookTabButton tabButton : this.tabButtons) {
                    if (tabButton.mouseClicked(event, doubleClick)) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        return this.searchBox != null && this.searchBox.isFocused() ? this.searchBox.mouseDragged(event, dx, dy) : false;
    }

    private boolean tryPlaceRecipe(final RecipeCollection recipeCollection, final RecipeDisplayId recipe, final boolean useMaxItems) {
        if (!recipeCollection.isCraftable(recipe) && recipe.equals(this.lastPlacedRecipe)) {
            return false;
        }

        this.lastPlacedRecipe = recipe;
        this.ghostSlots.clear();
        this.minecraft.gameMode.handlePlaceRecipe(this.minecraft.player.containerMenu.containerId, recipe, useMaxItems);
        return true;
    }

    private void onTabButtonPress(final Button button) {
        if (this.selectedTab != button && button instanceof RecipeBookTabButton recipeBookTabButton) {
            this.replaceSelected(recipeBookTabButton);
            this.updateCollections(true, this.isFiltering());
        }
    }

    private void replaceSelected(final RecipeBookTabButton tabButton) {
        if (this.selectedTab != null) {
            this.selectedTab.unselect();
        }

        tabButton.select();
        this.selectedTab = tabButton;
    }

    private void toggleFiltering() {
        RecipeBookType type = this.menu.getRecipeBookType();
        boolean newSetting = !this.book.isFiltering(type);
        this.book.setFiltering(type, newSetting);
    }

    public boolean hasClickedOutside(final double mx, final double my, final int leftPos, final int topPos, final int imageWidth, final int imageHeight) {
        if (!this.isVisible()) {
            return true;
        }

        boolean clickedOutside = mx < leftPos || my < topPos || mx >= leftPos + imageWidth || my >= topPos + imageHeight;
        boolean clickedOnRecipeBook = leftPos - 147 < mx && mx < leftPos && topPos < my && my < topPos + imageHeight;
        return clickedOutside && !clickedOnRecipeBook && !this.selectedTab.isHoveredOrFocused();
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        this.ignoreTextInput = false;
        if (!this.isVisible() || this.minecraft.player.isSpectator()) {
            return false;
        } else if (event.isEscape() && !this.isOffsetNextToMainGUI()) {
            this.setVisible(false);
            return true;
        } else if (this.searchBox.keyPressed(event)) {
            this.checkSearchStringUpdate();
            return true;
        } else if (this.searchBox.isFocused() && this.searchBox.isVisible() && !event.isEscape()) {
            return true;
        } else if (this.minecraft.options.keyChat.matches(event) && !this.searchBox.isFocused()) {
            this.ignoreTextInput = true;
            this.searchBox.setFocused(true);
            return true;
        } else if (event.isSelection() && this.lastRecipeCollection != null && this.lastRecipe != null) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            return this.tryPlaceRecipe(this.lastRecipeCollection, this.lastRecipe, event.hasShiftDown());
        } else {
            return false;
        }
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        this.ignoreTextInput = false;
        return GuiEventListener.super.keyReleased(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.ignoreTextInput) {
            return false;
        } else if (!this.isVisible() || this.minecraft.player.isSpectator()) {
            return false;
        } else if (this.searchBox.charTyped(event)) {
            this.checkSearchStringUpdate();
            return true;
        } else {
            return GuiEventListener.super.charTyped(event);
        }
    }

    @Override
    public boolean preeditUpdated(final @Nullable PreeditEvent event) {
        if (this.ignoreTextInput) {
            return false;
        } else if (!this.isVisible() || this.minecraft.player.isSpectator()) {
            return false;
        } else {
            return this.searchBox.preeditUpdated(event) ? true : GuiEventListener.super.preeditUpdated(event);
        }
    }

    @Override
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return false;
    }

    @Override
    public void setFocused(final boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    private void checkSearchStringUpdate() {
        String searchText = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        this.pirateSpeechForThePeople(searchText);
        if (!searchText.equals(this.lastSearch)) {
            this.updateCollections(false, this.isFiltering());
            this.lastSearch = searchText;
        }
    }

    private void pirateSpeechForThePeople(final String searchTarget) {
        if ("excitedze".equals(searchTarget)) {
            LanguageManager languageManager = this.minecraft.getLanguageManager();
            String arrrrCode = "en_pt";
            LanguageInfo language = languageManager.getLanguage("en_pt");
            if (language == null || languageManager.getSelected().equals("en_pt")) {
                return;
            }

            languageManager.setSelected("en_pt");
            this.minecraft.options.languageCode = "en_pt";
            this.minecraft.reloadResourcePacks();
            this.minecraft.options.save();
        }
    }

    private boolean isOffsetNextToMainGUI() {
        return this.xOffset == 86;
    }

    public void recipesUpdated() {
        this.selectMatchingRecipes();
        this.updateTabs(this.isFiltering());
        if (this.isVisible()) {
            this.updateCollections(false, this.isFiltering());
        }
    }

    public void recipeShown(final RecipeDisplayId recipe) {
        this.minecraft.player.removeRecipeHighlight(recipe);
    }

    public void fillGhostRecipe(final RecipeDisplay recipe) {
        this.ghostSlots.clear();
        ContextMap context = SlotDisplayContext.fromLevel(Objects.requireNonNull(this.minecraft.level));
        this.fillGhostRecipe(this.ghostSlots, recipe, context);
    }

    protected abstract void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context);

    protected void sendUpdateSettings() {
        if (this.minecraft.getConnection() != null) {
            RecipeBookType type = this.menu.getRecipeBookType();
            boolean open = this.book.getBookSettings().isOpen(type);
            boolean filtering = this.book.getBookSettings().isFiltering(type);
            this.minecraft.getConnection().send(new ServerboundRecipeBookChangeSettingsPacket(type, open, filtering));
        }
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.visible ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(final NarrationElementOutput output) {
        List<NarratableEntry> narratableEntries = Lists.newArrayList();
        this.recipeBookPage.listButtons(e -> {
            if (e.isActive()) {
                narratableEntries.add(e);
            }
        });
        narratableEntries.add(this.searchBox);
        narratableEntries.add(this.filterButton);
        narratableEntries.addAll(this.tabButtons);
        Screen.NarratableSearchResult narratable = Screen.findNarratableWidget(narratableEntries, null);
        if (narratable != null) {
            narratable.entry().updateNarration(output.nest());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record TabInfo(ItemStack primaryIcon, Optional<ItemStack> secondaryIcon, ExtendedRecipeBookCategory category) {
        public TabInfo(final SearchRecipeBookCategory category) {
            this(new ItemStack(Items.COMPASS), Optional.empty(), category);
        }

        public TabInfo(final Item icon, final RecipeBookCategory category) {
            this(new ItemStack(icon), Optional.empty(), category);
        }

        public TabInfo(final Item primaryIcon, final Item secondaryIcon, final RecipeBookCategory category) {
            this(new ItemStack(primaryIcon), Optional.of(new ItemStack(secondaryIcon)), category);
        }
    }
}