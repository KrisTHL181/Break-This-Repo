package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class BookSignScreen extends Screen {
   private static final Component EDIT_TITLE_LABEL = Component.translatable("book.editTitle");
   private static final Component FINALIZE_WARNING_LABEL = Component.translatable("book.finalizeWarning");
   private static final Component TITLE = Component.translatable("book.sign.title");
   private static final Component TITLE_EDIT_BOX = Component.translatable("book.sign.titlebox");
   private final BookEditScreen bookEditScreen;
   private final Player owner;
   private final List<String> pages;
   private final InteractionHand hand;
   private final Component ownerText;
   private EditBox titleBox;
   private String titleValue = "";

   public BookSignScreen(final BookEditScreen bookEditScreen, final Player owner, final InteractionHand hand, final List<String> pages) {
      super(TITLE);
      this.bookEditScreen = bookEditScreen;
      this.owner = owner;
      this.hand = hand;
      this.pages = pages;
      this.ownerText = Component.translatable("book.byAuthor", owner.getName()).withStyle(ChatFormatting.DARK_GRAY);
   }

   @Override
   protected void init() {
      Button finalizeButton = Button.builder(Component.translatable("book.finalizeButton"), button -> {
         this.saveChanges();
         this.minecraft.gui.setScreen(null);
      }).bounds(this.width / 2 - 100, 196, 98, 20).build();
      finalizeButton.active = false;
      this.titleBox = this.addRenderableWidget(new EditBox(this.minecraft.font, (this.width - 114) / 2 - 3, 50, 114, 20, TITLE_EDIT_BOX));
      this.titleBox.setMaxLength(15);
      this.titleBox.setBordered(false);
      this.titleBox.setCentered(true);
      this.titleBox.setTextColor(-16777216);
      this.titleBox.setTextShadow(false);
      this.titleBox.setResponder(value -> finalizeButton.active = !StringUtil.isBlank(value));
      this.titleBox.setValue(this.titleValue);
      this.addRenderableWidget(finalizeButton);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
         this.titleValue = this.titleBox.getValue();
         this.minecraft.gui.setScreen(this.bookEditScreen);
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.titleBox);
   }

   private void saveChanges() {
      int slot = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().getSelectedSlot() : 40;
      this.minecraft.getConnection().send(new ServerboundEditBookPacket(slot, this.pages, Optional.of(this.titleBox.getValue().trim())));
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (this.titleBox.isFocused() && !this.titleBox.getValue().isEmpty() && event.isConfirmation()) {
         this.saveChanges();
         this.minecraft.gui.setScreen(null);
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      int xo = (this.width - 192) / 2;
      int yo = 2;
      int titleHeaderWidth = this.font.width(EDIT_TITLE_LABEL);
      graphics.text(this.font, EDIT_TITLE_LABEL, xo + 36 + (114 - titleHeaderWidth) / 2, 34, -16777216, false);
      int nameWidth = this.font.width(this.ownerText);
      graphics.text(this.font, this.ownerText, xo + 36 + (114 - nameWidth) / 2, 60, -16777216, false);
      graphics.textWithWordWrap(this.font, FINALIZE_WARNING_LABEL, xo + 36, 82, 114, -16777216, false);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      graphics.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION, (this.width - 192) / 2, 2, 0.0F, 0.0F, 192, 192, 256, 256);
   }
}
