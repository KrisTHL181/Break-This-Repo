package net.minecraft.client.gui.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatScreen extends Screen {
   public static final double MOUSE_SCROLL_SPEED = 7.0;
   private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
   private String historyBuffer = "";
   private int historyPos = -1;
   protected EditBox input;
   protected String initial;
   protected boolean isDraft;
   protected ChatScreen.ExitReason exitReason = ChatScreen.ExitReason.INTERRUPTED;
   CommandSuggestions commandSuggestions;

   public ChatScreen(String p_95579_, boolean p_430033_) {
      super(Component.translatable("chat_screen.title"));
      this.initial = p_95579_;
      this.isDraft = p_430033_;
   }

   @Override
   protected void init() {
      this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
      this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
         }
      };
      this.input.setMaxLength(256);
      this.input.setBordered(false);
      this.input.setValue(this.initial);
      this.input.setResponder(this::onEdited);
      this.input.addFormatter(this::formatChat);
      this.input.setCanLoseFocus(false);
      this.addRenderableWidget(this.input);
      this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
      this.commandSuggestions.setAllowHiding(false);
      this.commandSuggestions.setAllowSuggestions(false);
      this.commandSuggestions.updateCommandInfo();
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.input);
   }

   @Override
   public void resize(int p_95601_, int p_95602_) {
      this.initial = this.input.getValue();
      this.init(p_95601_, p_95602_);
   }

   @Override
   public void onClose() {
      this.exitReason = ChatScreen.ExitReason.INTENTIONAL;
      super.onClose();
   }

   @Override
   public void removed() {
      this.minecraft.gui.getChat().resetChatScroll();
      this.initial = this.input.getValue();
      if (this.shouldDiscardDraft() || StringUtils.isBlank(this.initial)) {
         this.minecraft.gui.getChat().discardDraft();
      } else if (!this.isDraft) {
         this.minecraft.gui.getChat().saveAsDraft(this.initial);
      }
   }

   protected boolean shouldDiscardDraft() {
      return this.exitReason != ChatScreen.ExitReason.INTERRUPTED
         && (this.exitReason != ChatScreen.ExitReason.INTENTIONAL || !this.minecraft.options.saveChatDrafts().get());
   }

   private void onEdited(String p_95611_) {
      this.commandSuggestions.setAllowSuggestions(true);
      this.commandSuggestions.updateCommandInfo();
      this.isDraft = false;
   }

   @Override
   public boolean keyPressed(KeyEvent p_426273_) {
      if (this.commandSuggestions.keyPressed(p_426273_)) {
         return true;
      }

      if (this.isDraft && p_426273_.key() == 259) {
         this.input.setValue("");
         this.isDraft = false;
         return true;
      }

      if (super.keyPressed(p_426273_)) {
         return true;
      }

      if (p_426273_.isConfirmation()) {
         this.handleChatInput(this.input.getValue(), true);
         this.exitReason = ChatScreen.ExitReason.DONE;
         this.minecraft.setScreen(null);
         return true;
      }

      switch (p_426273_.key()) {
         case 264:
            this.moveInHistory(1);
            break;
         case 265:
            this.moveInHistory(-1);
            break;
         case 266:
            this.minecraft.gui.getChat().scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
            break;
         case 267:
            this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
            break;
         default:
            return false;
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double p_95581_, double p_95582_, double p_95583_, double p_300876_) {
      p_300876_ = Mth.clamp(p_300876_, -1.0, 1.0);
      if (this.commandSuggestions.mouseScrolled(p_300876_)) {
         return true;
      }

      if (!this.minecraft.hasShiftDown()) {
         p_300876_ *= 7.0;
      }

      this.minecraft.gui.getChat().scrollChat((int)p_300876_);
      return true;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_429485_, boolean p_423918_) {
      if (this.commandSuggestions.mouseClicked(p_429485_)) {
         return true;
      }

      if (p_429485_.button() == 0) {
         int i = this.minecraft.getWindow().getGuiScaledHeight();
         ActiveTextCollector.ClickableStyleFinder activetextcollector$clickablestylefinder = new ActiveTextCollector.ClickableStyleFinder(
               this.getFont(), (int)p_429485_.x(), (int)p_429485_.y()
            )
            .includeInsertions(this.insertionClickMode());
         this.minecraft.gui.getChat().captureClickableText(activetextcollector$clickablestylefinder, i, this.minecraft.gui.getGuiTicks(), true);
         Style style = activetextcollector$clickablestylefinder.result();
         if (style != null && this.handleComponentClicked(style, this.insertionClickMode())) {
            this.initial = this.input.getValue();
            return true;
         }
      }

      return super.mouseClicked(p_429485_, p_423918_);
   }

   private boolean insertionClickMode() {
      return this.minecraft.hasShiftDown();
   }

   private boolean handleComponentClicked(Style p_455754_, boolean p_451271_) {
      ClickEvent clickevent = p_455754_.getClickEvent();
      if (p_451271_) {
         if (p_455754_.getInsertion() != null) {
            this.insertText(p_455754_.getInsertion(), false);
         }
      } else if (clickevent != null) {
         if (clickevent instanceof ClickEvent.Custom clickevent$custom && clickevent$custom.id().equals(ChatComponent.QUEUE_EXPAND_ID)) {
            ChatListener chatlistener = this.minecraft.getChatListener();
            if (chatlistener.queueSize() != 0L) {
               chatlistener.acceptNextDelayedMessage();
            }
         } else {
            defaultHandleGameClickEvent(clickevent, this.minecraft, this);
         }

         return true;
      }

      return false;
   }

   @Override
   public void insertText(String p_95606_, boolean p_95607_) {
      if (p_95607_) {
         this.input.setValue(p_95606_);
      } else {
         this.input.insertText(p_95606_);
      }
   }

   public void moveInHistory(int p_95589_) {
      int i = this.historyPos + p_95589_;
      int j = this.minecraft.gui.getChat().getRecentChat().size();
      i = Mth.clamp(i, 0, j);
      if (i != this.historyPos) {
         if (i == j) {
            this.historyPos = j;
            this.input.setValue(this.historyBuffer);
         } else {
            if (this.historyPos == j) {
               this.historyBuffer = this.input.getValue();
            }

            this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(i));
            this.commandSuggestions.setAllowSuggestions(false);
            this.historyPos = i;
         }
      }
   }

   private @Nullable FormattedCharSequence formatChat(String p_429271_, int p_423659_) {
      return this.isDraft ? FormattedCharSequence.forward(p_429271_, Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)) : null;
   }

   @Override
   public void render(GuiGraphics p_282470_, int p_282674_, int p_282014_, float p_283132_) {
      p_282470_.fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
      this.minecraft.gui.getChat().render(p_282470_, this.font, this.minecraft.gui.getGuiTicks(), p_282674_, p_282014_, true, this.insertionClickMode());
      super.render(p_282470_, p_282674_, p_282014_, p_283132_);
      this.commandSuggestions.render(p_282470_, p_282674_, p_282014_);
   }

   @Override
   public void renderBackground(GuiGraphics p_298203_, int p_299897_, int p_297752_, float p_300216_) {
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean isAllowedInPortal() {
      return true;
   }

   @Override
   protected void updateNarrationState(NarrationElementOutput p_169238_) {
      p_169238_.add(NarratedElementType.TITLE, this.getTitle());
      p_169238_.add(NarratedElementType.USAGE, USAGE_TEXT);
      String s = this.input.getValue();
      if (!s.isEmpty()) {
         p_169238_.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", s));
      }
   }

   public void handleChatInput(String p_242400_, boolean p_242161_) {
      p_242400_ = this.normalizeChatMessage(p_242400_);
      if (!p_242400_.isEmpty()) {
         if (p_242161_) {
            this.minecraft.gui.getChat().addRecentChat(p_242400_);
         }

         if (p_242400_.startsWith("/")) {
            this.minecraft.player.connection.sendCommand(p_242400_.substring(1));
         } else {
            this.minecraft.player.connection.sendChat(p_242400_);
         }
      }
   }

   public String normalizeChatMessage(String p_232707_) {
      return StringUtil.trimChatMessage(StringUtils.normalizeSpace(p_232707_.trim()));
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface ChatConstructor<T extends ChatScreen> {
      T create(String var1, boolean var2);
   }

   @OnlyIn(Dist.CLIENT)
   protected enum ExitReason {
      INTENTIONAL,
      INTERRUPTED,
      DONE;
   }
}
