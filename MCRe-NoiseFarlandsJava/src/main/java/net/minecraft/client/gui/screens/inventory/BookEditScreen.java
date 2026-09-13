package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BookEditScreen extends Screen {
    public static final int TEXT_WIDTH = 114;
    public static final int TEXT_HEIGHT = 126;
    public static final int IMAGE_WIDTH = 192;
    public static final int IMAGE_HEIGHT = 192;
    public static final int BACKGROUND_TEXTURE_WIDTH = 256;
    public static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final int MENU_BUTTON_MARGIN = 4;
    private static final int MENU_BUTTON_SIZE = 98;
    private static final int PAGE_BUTTON_Y = 157;
    private static final int PAGE_BACK_BUTTON_X = 43;
    private static final int PAGE_FORWARD_BUTTON_X = 116;
    private static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
    private static final int PAGE_INDICATOR_X_OFFSET = 148;
    private static final Component TITLE = Component.translatable("book.edit.title");
    private static final Component SIGN_BOOK_LABEL = Component.translatable("book.signButton");
    private final Player owner;
    private final ItemStack book;
    private final BookSignScreen signScreen;
    private int currentPage;
    private final List<String> pages = Lists.newArrayList();
    private PageButton forwardButton;
    private PageButton backButton;
    private final InteractionHand hand;
    private Component numberOfPages = CommonComponents.EMPTY;
    private MultiLineEditBox page;

    public BookEditScreen(final Player owner, final ItemStack book, final InteractionHand hand, final WritableBookContent content) {
        super(TITLE);
        this.owner = owner;
        this.book = book;
        this.hand = hand;
        content.getPages(Minecraft.getInstance().isTextFilteringEnabled()).forEach(this.pages::add);
        if (this.pages.isEmpty()) {
            this.pages.add("");
        }

        this.signScreen = new BookSignScreen(this, owner, hand, this.pages);
    }

    private int getNumPages() {
        return this.pages.size();
    }

    @Override
    protected void init() {
        int left = this.backgroundLeft();
        int top = this.backgroundTop();
        int padding = 8;
        this.page = MultiLineEditBox.builder()
            .setShowDecorations(false)
            .setTextColor(-16777216)
            .setCursorColor(-16777216)
            .setShowBackground(false)
            .setTextShadow(false)
            .setX((this.width - 114) / 2 - 8)
            .setY(28)
            .build(this.font, 122, 134, CommonComponents.EMPTY);
        this.page.setCharacterLimit(1024);
        this.page.setLineLimit(126 / 9);
        this.page.setValueListener(value -> this.pages.set(this.currentPage, value));
        this.addRenderableWidget(this.page);
        this.updatePageContent();
        this.numberOfPages = this.getPageNumberMessage();
        this.backButton = this.addRenderableWidget(new PageButton(left + 43, top + 157, false, button -> this.pageBack(), true));
        this.forwardButton = this.addRenderableWidget(new PageButton(left + 116, top + 157, true, button -> this.pageForward(), true));
        this.addRenderableWidget(
            Button.builder(SIGN_BOOK_LABEL, button -> this.minecraft.gui.setScreen(this.signScreen))
                .pos(this.width / 2 - 98 - 2, this.menuControlsTop())
                .width(98)
                .build()
        );
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.minecraft.gui.setScreen(null);
            this.saveChanges();
        }).pos(this.width / 2 + 2, this.menuControlsTop()).width(98).build());
        this.updateButtonVisibility();
    }

    private int backgroundLeft() {
        return (this.width - 192) / 2;
    }

    private int backgroundTop() {
        return 2;
    }

    private int menuControlsTop() {
        return this.backgroundTop() + 192 + 2;
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.page);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.getPageNumberMessage());
    }

    private Component getPageNumberMessage() {
        return Component.translatable("book.pageIndicator", this.currentPage + 1, this.getNumPages()).withColor(-16777216).withoutShadow();
    }

    private void pageBack() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.updatePageContent();
        }

        this.updateButtonVisibility();
    }

    private void pageForward() {
        if (this.currentPage < this.getNumPages() - 1) {
            this.currentPage++;
        } else {
            this.appendPageToBook();
            if (this.currentPage < this.getNumPages() - 1) {
                this.currentPage++;
            }
        }

        this.updatePageContent();
        this.updateButtonVisibility();
    }

    private void updatePageContent() {
        this.page.setValue(this.pages.get(this.currentPage), true);
        this.numberOfPages = this.getPageNumberMessage();
    }

    private void updateButtonVisibility() {
        this.backButton.visible = this.currentPage > 0;
    }

    private void eraseEmptyTrailingPages() {
        ListIterator<String> pagesIt = this.pages.listIterator(this.pages.size());

        while (pagesIt.hasPrevious() && pagesIt.previous().isEmpty()) {
            pagesIt.remove();
        }
    }

    private void saveChanges() {
        this.eraseEmptyTrailingPages();
        this.updateLocalCopy();
        int slot = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().getSelectedSlot() : 40;
        this.minecraft.getConnection().send(new ServerboundEditBookPacket(slot, this.pages, Optional.empty()));
    }

    private void updateLocalCopy() {
        this.book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(this.pages.stream().map(Filterable::passThrough).toList()));
    }

    private void appendPageToBook() {
        if (this.getNumPages() < 100) {
            this.pages.add("");
        }
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        switch (event.key()) {
            case 266:
                this.backButton.onPress(event);
                return true;
            case 267:
                this.forwardButton.onPress(event);
                return true;
            default:
                return super.keyPressed(event);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.visitText(graphics.textRenderer());
    }

    private void visitText(final ActiveTextCollector collector) {
        int left = this.backgroundLeft();
        int top = this.backgroundTop();
        collector.accept(TextAlignment.RIGHT, left + 148, top + 16, this.numberOfPages);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION, this.backgroundLeft(), this.backgroundTop(), 0.0F, 0.0F, 192, 192, 256, 256);
    }
}