package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RecipeBookPage {
    public static final int ITEMS_PER_PAGE = 20;
    private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("recipe_book/page_forward"), Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted")
    );
    private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("recipe_book/page_backward"), Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted")
    );
    private static final Component NEXT_PAGE_TEXT = Component.translatable("gui.recipebook.next_page");
    private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("gui.recipebook.previous_page");
    private static final int TURN_PAGE_SPRITE_WIDTH = 12;
    private static final int TURN_PAGE_SPRITE_HEIGHT = 17;
    private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
    private @Nullable RecipeButton hoveredButton;
    private final OverlayRecipeComponent overlay;
    private Minecraft minecraft;
    private final RecipeBookComponent<?> parent;
    private List<RecipeCollection> recipeCollections = ImmutableList.of();
    private @Nullable ImageButton forwardButton;
    private @Nullable ImageButton backButton;
    private int totalPages;
    private int currentPage;
    private ClientRecipeBook recipeBook;
    private @Nullable RecipeDisplayId lastClickedRecipe;
    private @Nullable RecipeCollection lastClickedRecipeCollection;
    private boolean isFiltering;

    public RecipeBookPage(final RecipeBookComponent<?> parent, final SlotSelectTime slotSelectTime, final boolean isFurnaceMenu) {
        this.parent = parent;
        this.overlay = new OverlayRecipeComponent(slotSelectTime, isFurnaceMenu);

        for (int i = 0; i < 20; i++) {
            this.buttons.add(new RecipeButton(slotSelectTime));
        }
    }

    public void init(final Minecraft minecraft, final int xo, final int yo) {
        this.minecraft = minecraft;
        this.recipeBook = minecraft.player.getRecipeBook();

        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).setPosition(xo + 11 + 25 * (i % 5), yo + 31 + 25 * (i / 5));
        }

        this.forwardButton = new ImageButton(xo + 93, yo + 137, 12, 17, PAGE_FORWARD_SPRITES, button -> this.updateArrowButtons(), NEXT_PAGE_TEXT);
        this.forwardButton.setTooltip(Tooltip.create(NEXT_PAGE_TEXT));
        this.backButton = new ImageButton(xo + 38, yo + 137, 12, 17, PAGE_BACKWARD_SPRITES, button -> this.updateArrowButtons(), PREVIOUS_PAGE_TEXT);
        this.backButton.setTooltip(Tooltip.create(PREVIOUS_PAGE_TEXT));
    }

    public void updateCollections(final List<RecipeCollection> recipeCollections, final boolean resetPage, final boolean isFiltering) {
        this.recipeCollections = recipeCollections;
        this.isFiltering = isFiltering;
        this.totalPages = (int)Math.ceil(recipeCollections.size() / 20.0);
        if (this.totalPages <= this.currentPage || resetPage) {
            this.currentPage = 0;
        }

        this.updateButtonsForPage();
    }

    private void updateButtonsForPage() {
        int startOffset = 20 * this.currentPage;
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);

        for (int i = 0; i < this.buttons.size(); i++) {
            RecipeButton button = this.buttons.get(i);
            if (startOffset + i < this.recipeCollections.size()) {
                RecipeCollection recipeCollection = this.recipeCollections.get(startOffset + i);
                button.init(recipeCollection, this.isFiltering, this, context);
                button.visible = true;
            } else {
                button.visible = false;
            }
        }

        this.updateArrowButtons();
    }

    private void updateArrowButtons() {
        if (this.forwardButton != null) {
            this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
        }

        if (this.backButton != null) {
            this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
        }
    }

    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xo, final int yo, final int mouseX, final int mouseY, final float a) {
        if (this.totalPages > 1) {
            Component pageNumbers = Component.translatable("gui.recipebook.page", this.currentPage + 1, this.totalPages);
            int pWidth = this.minecraft.font.width(pageNumbers);
            graphics.text(this.minecraft.font, pageNumbers, xo - pWidth / 2 + 73, yo + 141, -1);
        }

        this.hoveredButton = null;

        for (RecipeButton recipeBookButton : this.buttons) {
            recipeBookButton.extractRenderState(graphics, mouseX, mouseY, a);
            if (recipeBookButton.visible && recipeBookButton.isHoveredOrFocused()) {
                this.hoveredButton = recipeBookButton;
            }
        }

        if (this.forwardButton != null) {
            this.forwardButton.extractRenderState(graphics, mouseX, mouseY, a);
        }

        if (this.backButton != null) {
            this.backButton.extractRenderState(graphics, mouseX, mouseY, a);
        }

        graphics.nextStratum();
        this.overlay.extractRenderState(graphics, mouseX, mouseY, a);
    }

    public void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (this.minecraft.gui.screen() != null && this.hoveredButton != null && !this.overlay.isVisible()) {
            ItemStack displayStack = this.hoveredButton.getDisplayStack();
            Identifier tooltipStyle = displayStack.get(DataComponents.TOOLTIP_STYLE);
            graphics.setComponentTooltipForNextFrame(this.minecraft.font, this.hoveredButton.getTooltipText(displayStack), mouseX, mouseY, tooltipStyle);
        }
    }

    public @Nullable RecipeDisplayId getLastClickedRecipe() {
        return this.lastClickedRecipe;
    }

    public @Nullable RecipeCollection getLastClickedRecipeCollection() {
        return this.lastClickedRecipeCollection;
    }

    public void setInvisible() {
        this.overlay.setVisible(false);
    }

    public boolean mouseClicked(
        final MouseButtonEvent event, final int xo, final int yo, final int imageWidth, final int imageHeight, final boolean doubleClick
    ) {
        this.lastClickedRecipe = null;
        this.lastClickedRecipeCollection = null;
        if (this.overlay.isVisible()) {
            if (this.overlay.mouseClicked(event, doubleClick)) {
                this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
                this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
            } else {
                this.overlay.setVisible(false);
            }

            return true;
        } else {
            if (this.forwardButton.mouseClicked(event, doubleClick)) {
                this.currentPage++;
                this.updateButtonsForPage();
                return true;
            }

            if (this.backButton.mouseClicked(event, doubleClick)) {
                this.currentPage--;
                this.updateButtonsForPage();
                return true;
            }

            ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);

            for (RecipeButton button : this.buttons) {
                if (button.mouseClicked(event, doubleClick)) {
                    if (event.button() == 0) {
                        this.lastClickedRecipe = button.getCurrentRecipe();
                        this.lastClickedRecipeCollection = button.getCollection();
                    } else if (event.button() == 1 && !this.overlay.isVisible() && !button.isOnlyOption()) {
                        this.overlay
                            .init(
                                button.getCollection(),
                                context,
                                this.isFiltering,
                                button.getX(),
                                button.getY(),
                                xo + imageWidth / 2,
                                yo + 13 + imageHeight / 2,
                                button.getWidth()
                            );
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public void recipeShown(final RecipeDisplayId recipe) {
        this.parent.recipeShown(recipe);
    }

    public ClientRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    protected void listButtons(final Consumer<AbstractWidget> buttonConsumer) {
        this.buttons.forEach(buttonConsumer);
    }
}