package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   public BookSignScreen(BookEditScreen p_410129_, Player p_409600_, InteractionHand p_406590_, List<String> p_406352_) {
      super(TITLE);
      this.bookEditScreen = p_410129_;
      this.owner = p_409600_;
      this.hand = p_406590_;
      this.pages = p_406352_;
      this.ownerText = Component.translatable("book.byAuthor", p_409600_.getName()).withStyle(ChatFormatting.DARK_GRAY);
   }

   @Override
   protected void init() {
      Button button = Button.builder(Component.translatable("book.finalizeButton"), p_408707_ -> {
         this.saveChanges();
         this.minecraft.setScreen(null);
      }).bounds(this.width / 2 - 100, 196, 98, 20).build();
      button.active = false;
      this.titleBox = this.addRenderableWidget(new EditBox(this.minecraft.font, (this.width - 114) / 2 - 3, 50, 114, 20, TITLE_EDIT_BOX));
      this.titleBox.setMaxLength(15);
      this.titleBox.setBordered(false);
      this.titleBox.setCentered(true);
      this.titleBox.setTextColor(-16777216);
      this.titleBox.setTextShadow(false);
      this.titleBox.setResponder(p_408022_ -> button.active = !StringUtil.isBlank(p_408022_));
      this.titleBox.setValue(this.titleValue);
      this.addRenderableWidget(button);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_408411_ -> {
         this.titleValue = this.titleBox.getValue();
         this.minecraft.setScreen(this.bookEditScreen);
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.titleBox);
   }

   private void saveChanges() {
      int i = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().getSelectedSlot() : 40;
      this.minecraft.getConnection().send(new ServerboundEditBookPacket(i, this.pages, Optional.of(this.titleBox.getValue().trim())));
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent p_430759_) {
      if (this.titleBox.isFocused() && !this.titleBox.getValue().isEmpty() && p_430759_.isConfirmation()) {
         this.saveChanges();
         this.minecraft.setScreen(null);
         return true;
      } else {
         return super.keyPressed(p_430759_);
      }
   }

   @Override
   public void render(GuiGraphics p_410592_, int p_407435_, int p_406908_, float p_408968_) {
      super.render(p_410592_, p_407435_, p_406908_, p_408968_);
      int i = (this.width - 192) / 2;
      int j = 2;
      int k = this.font.width(EDIT_TITLE_LABEL);
      p_410592_.drawString(this.font, EDIT_TITLE_LABEL, i + 36 + (114 - k) / 2, 34, -16777216, false);
      int l = this.font.width(this.ownerText);
      p_410592_.drawString(this.font, this.ownerText, i + 36 + (114 - l) / 2, 60, -16777216, false);
      p_410592_.drawWordWrap(this.font, FINALIZE_WARNING_LABEL, i + 36, 82, 114, -16777216, false);
   }

   @Override
   public void renderBackground(GuiGraphics p_409264_, int p_410109_, int p_407382_, float p_408375_) {
      super.renderBackground(p_409264_, p_410109_, p_407382_, p_408375_);
      p_409264_.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION, (this.width - 192) / 2, 2, 0.0F, 0.0F, 192, 192, 256, 256);
   }
}
