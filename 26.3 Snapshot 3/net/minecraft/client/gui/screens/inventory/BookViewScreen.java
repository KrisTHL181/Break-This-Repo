package net.minecraft.client.gui.screens.inventory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jspecify.annotations.Nullable;

public class BookViewScreen extends Screen {
   public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
   public static final int PAGE_TEXT_X_OFFSET = 36;
   public static final int PAGE_TEXT_Y_OFFSET = 30;
   private static final int BACKGROUND_TEXTURE_WIDTH = 256;
   private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
   private static final Component TITLE = Component.translatable("book.view.title");
   private static final Style PAGE_TEXT_STYLE = Style.EMPTY.withoutShadow().withColor(-16777216);
   public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess(List.of());
   public static final Identifier BOOK_LOCATION = Identifier.withDefaultNamespace("textures/gui/book.png");
   protected static final int TEXT_WIDTH = 114;
   protected static final int TEXT_HEIGHT = 128;
   protected static final int IMAGE_WIDTH = 192;
   private static final int PAGE_INDICATOR_X_OFFSET = 148;
   protected static final int IMAGE_HEIGHT = 192;
   private static final int PAGE_BUTTON_Y = 157;
   private static final int PAGE_BACK_BUTTON_X = 43;
   private static final int PAGE_FORWARD_BUTTON_X = 116;
   private BookViewScreen.BookAccess bookAccess;
   private int currentPage;
   private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
   private int cachedPage = -1;
   private Component pageMsg = CommonComponents.EMPTY;
   private PageButton forwardButton;
   private PageButton backButton;
   private final boolean playTurnSound;

   public BookViewScreen(final BookViewScreen.BookAccess bookAccess) {
      this(bookAccess, true);
   }

   public BookViewScreen() {
      this(EMPTY_ACCESS, false);
   }

   private BookViewScreen(final BookViewScreen.BookAccess bookAccess, final boolean playTurnSound) {
      super(TITLE);
      this.bookAccess = bookAccess;
      this.playTurnSound = playTurnSound;
   }

   public void setBookAccess(final BookViewScreen.BookAccess bookAccess) {
      this.bookAccess = bookAccess;
      this.currentPage = Mth.clamp(this.currentPage, 0, bookAccess.getPageCount());
      this.updateButtonVisibility();
      this.cachedPage = -1;
   }

   public boolean setPage(final int page) {
      int clampedPage = Mth.clamp(page, 0, this.bookAccess.getPageCount() - 1);
      if (clampedPage != this.currentPage) {
         this.currentPage = clampedPage;
         this.updateButtonVisibility();
         this.cachedPage = -1;
         return true;
      } else {
         return false;
      }
   }

   protected boolean forcePage(final int page) {
      return this.setPage(page);
   }

   @Override
   protected void init() {
      this.createMenuControls();
      this.createPageControlButtons();
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinLines(super.getNarrationMessage(), this.getPageNumberMessage(), this.bookAccess.getPage(this.currentPage));
   }

   private Component getPageNumberMessage() {
      return Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1)).withStyle(PAGE_TEXT_STYLE);
   }

   protected void createMenuControls() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).pos((this.width - 200) / 2, this.menuControlsTop()).width(200).build()
      );
   }

   protected void createPageControlButtons() {
      int left = this.backgroundLeft();
      int top = this.backgroundTop();
      this.forwardButton = this.addRenderableWidget(new PageButton(left + 116, top + 157, true, button -> this.pageForward(), this.playTurnSound));
      this.backButton = this.addRenderableWidget(new PageButton(left + 43, top + 157, false, button -> this.pageBack(), this.playTurnSound));
      this.updateButtonVisibility();
   }

   private int getNumPages() {
      return this.bookAccess.getPageCount();
   }

   protected void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
   }

   protected void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      }

      this.updateButtonVisibility();
   }

   private void updateButtonVisibility() {
      this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
      this.backButton.visible = this.currentPage > 0;
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (super.keyPressed(event)) {
         return true;
      }

      return switch (event.key()) {
         case 266 -> {
            this.backButton.onPress(event);
            yield true;
         }
         case 267 -> {
            this.forwardButton.onPress(event);
            yield true;
         }
         default -> false;
      };
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      this.visitText(graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR), false);
   }

   private void visitText(final ActiveTextCollector collector, final boolean clickableOnly) {
      if (this.cachedPage != this.currentPage) {
         FormattedText pageText = ComponentUtils.mergeStyles(this.bookAccess.getPage(this.currentPage), PAGE_TEXT_STYLE);
         this.cachedPageComponents = this.font.split(pageText, 114);
         this.pageMsg = this.getPageNumberMessage();
         this.cachedPage = this.currentPage;
      }

      int left = this.backgroundLeft();
      int top = this.backgroundTop();
      if (!clickableOnly) {
         collector.accept(TextAlignment.RIGHT, left + 148, top + 16, this.pageMsg);
      }

      int shownLines = Math.min(128 / 9, this.cachedPageComponents.size());

      for (int i = 0; i < shownLines; i++) {
         FormattedCharSequence component = this.cachedPageComponents.get(i);
         collector.accept(left + 36, top + 30 + i * 9, component);
      }
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION, this.backgroundLeft(), this.backgroundTop(), 0.0F, 0.0F, 192, 192, 256, 256);
   }

   private int backgroundLeft() {
      return (this.width - 192) / 2;
   }

   private int backgroundTop() {
      return 2;
   }

   protected int menuControlsTop() {
      return this.backgroundTop() + 192 + 2;
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (event.button() == 0) {
         ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.font, (int)event.x(), (int)event.y());
         this.visitText(finder, true);
         Style clickedStyle = finder.result();
         if (clickedStyle != null && this.handleClickEvent(clickedStyle.getClickEvent())) {
            return true;
         }
      }

      return super.mouseClicked(event, doubleClick);
   }

   protected boolean handleClickEvent(final @Nullable ClickEvent event) {
      if (event == null) {
         return false;
      }

      LocalPlayer player = Objects.requireNonNull(this.minecraft.player, "Player not available");
      switch (event) {
         case ClickEvent.ChangePage(int page):
            this.forcePage(page - 1);
            break;
         case ClickEvent.RunCommand(String command):
            this.closeContainerOnServer();
            clickCommandAction(player, command, null);
            break;
         default:
            defaultHandleGameClickEvent(event, this.minecraft, this);
      }

      return true;
   }

   protected void closeContainerOnServer() {
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   public record BookAccess(List<Component> pages) {
      public int getPageCount() {
         return this.pages.size();
      }

      public Component getPage(final int page) {
         return page >= 0 && page < this.getPageCount() ? this.pages.get(page) : CommonComponents.EMPTY;
      }

      public static BookViewScreen.@Nullable BookAccess fromItem(final ItemStack itemStack) {
         boolean filterEnabled = Minecraft.getInstance().isTextFilteringEnabled();
         WrittenBookContent writtenContent = itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
         if (writtenContent != null) {
            return new BookViewScreen.BookAccess(writtenContent.getPages(filterEnabled));
         }

         WritableBookContent writableContent = itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
         return writableContent != null ? new BookViewScreen.BookAccess(writableContent.getPages(filterEnabled).map(Component::literal).toList()) : null;
      }
   }
}
