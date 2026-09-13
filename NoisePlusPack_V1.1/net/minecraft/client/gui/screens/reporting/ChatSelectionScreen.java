package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionScreen extends Screen {
   static final Identifier CHECKMARK_SPRITE = Identifier.withDefaultNamespace("icon/checkmark");
   private static final Component TITLE = Component.translatable("gui.chatSelection.title");
   private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
   private final @Nullable Screen lastScreen;
   private final ReportingContext reportingContext;
   private Button confirmSelectedButton;
   private MultiLineLabel contextInfoLabel;
   private ChatSelectionScreen.@Nullable ChatSelectionList chatSelectionList;
   final ChatReport.Builder report;
   private final Consumer<ChatReport.Builder> onSelected;
   private ChatSelectionLogFiller chatLogFiller;

   public ChatSelectionScreen(@Nullable Screen p_239090_, ReportingContext p_239091_, ChatReport.Builder p_298838_, Consumer<ChatReport.Builder> p_239093_) {
      super(TITLE);
      this.lastScreen = p_239090_;
      this.reportingContext = p_239091_;
      this.report = p_298838_.copy();
      this.onSelected = p_239093_;
   }

   @Override
   protected void init() {
      this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
      this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
      this.chatSelectionList = this.addRenderableWidget(
         new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9)
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, p_239860_ -> this.onClose()).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build()
      );
      this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_296214_ -> {
         this.onSelected.accept(this.report);
         this.onClose();
      }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
      this.updateConfirmSelectedButton();
      this.extendLog();
      this.chatSelectionList.setScrollAmount(this.chatSelectionList.maxScrollAmount());
   }

   private boolean canReport(LoggedChatMessage p_242240_) {
      return p_242240_.canReport(this.report.reportedProfileId());
   }

   private void extendLog() {
      int i = this.chatSelectionList.getMaxVisibleEntries();
      this.chatLogFiller.fillNextPage(i, this.chatSelectionList);
   }

   void onReachedScrollTop() {
      this.extendLog();
   }

   void updateConfirmSelectedButton() {
      this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
   }

   @Override
   public void render(GuiGraphics p_282899_, int p_239287_, int p_239288_, float p_239289_) {
      super.render(p_282899_, p_239287_, p_239288_, p_239289_);
      ActiveTextCollector activetextcollector = p_282899_.textRenderer();
      p_282899_.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
      AbuseReportLimits abusereportlimits = this.reportingContext.sender().reportLimits();
      int i = this.report.reportedMessages().size();
      int j = abusereportlimits.maxReportedMessageCount();
      Component component = Component.translatable("gui.chatSelection.selected", i, j);
      p_282899_.drawCenteredString(this.font, component, this.width / 2, 26, -1);
      int k = this.chatSelectionList.getFooterTop();
      this.contextInfoLabel.visitLines(TextAlignment.CENTER, this.width / 2, k, 9, activetextcollector);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
   }

   @OnlyIn(Dist.CLIENT)
   public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
      public static final int ITEM_HEIGHT = 16;
      private ChatSelectionScreen.ChatSelectionList.@Nullable Heading previousHeading;

      public ChatSelectionList(final Minecraft p_239060_, final int p_239061_) {
         super(p_239060_, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height - p_239061_ - 80, 40, 16);
      }

      @Override
      public void setScrollAmount(double p_239021_) {
         double d0 = this.scrollAmount();
         super.setScrollAmount(p_239021_);
         if (this.maxScrollAmount() > 1.0E-5F && p_239021_ <= 1.0E-5F && !Mth.equal(p_239021_, d0)) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }
      }

      @Override
      public void acceptMessage(int p_242846_, LoggedChatMessage.Player p_242909_) {
         boolean flag = p_242909_.canReport(ChatSelectionScreen.this.report.reportedProfileId());
         ChatTrustLevel chattrustlevel = p_242909_.trustLevel();
         GuiMessageTag guimessagetag = chattrustlevel.createTag(p_242909_.message());
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageEntry(
            p_242846_, p_242909_.toContentComponent(), p_242909_.toNarrationComponent(), guimessagetag, flag, true
         );
         this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
         this.updateHeading(p_242909_, flag);
      }

      private void updateHeading(LoggedChatMessage.Player p_242229_, boolean p_240019_) {
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(
            p_242229_.profile(), p_242229_.toHeadingComponent(), p_240019_
         );
         this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
         ChatSelectionScreen.ChatSelectionList.Heading chatselectionscreen$chatselectionlist$heading = new ChatSelectionScreen.ChatSelectionList.Heading(
            p_242229_.profileId(), chatselectionscreen$chatselectionlist$entry
         );
         if (this.previousHeading != null && this.previousHeading.canCombine(chatselectionscreen$chatselectionlist$heading)) {
            this.removeEntryFromTop(this.previousHeading.entry());
         }

         this.previousHeading = chatselectionscreen$chatselectionlist$heading;
      }

      @Override
      public void acceptDivider(Component p_239876_) {
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(p_239876_));
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.previousHeading = null;
      }

      @Override
      public int getRowWidth() {
         return Math.min(350, this.width - 50);
      }

      public int getMaxVisibleEntries() {
         return Mth.positiveCeilDiv(this.height, 16);
      }

      protected void renderItem(GuiGraphics p_281532_, int p_239775_, int p_239776_, float p_239777_, ChatSelectionScreen.ChatSelectionList.Entry p_426876_) {
         if (this.shouldHighlightEntry(p_426876_)) {
            boolean flag = this.getSelected() == p_426876_;
            int i = this.isFocused() && flag ? -1 : -8355712;
            this.renderSelection(p_281532_, p_426876_, i);
         }

         p_426876_.renderContent(p_281532_, p_239775_, p_239776_, this.getHovered() == p_426876_, p_239777_);
      }

      private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry p_240327_) {
         if (p_240327_.canSelect()) {
            boolean flag = this.getSelected() == p_240327_;
            boolean flag1 = this.getSelected() == null;
            boolean flag2 = this.getHovered() == p_240327_;
            return flag || flag1 && flag2 && p_240327_.canReport();
         } else {
            return false;
         }
      }

      protected ChatSelectionScreen.ChatSelectionList.@Nullable Entry nextEntry(ScreenDirection p_265203_) {
         return this.nextEntry(p_265203_, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
      }

      public void setSelected(ChatSelectionScreen.ChatSelectionList.@Nullable Entry p_265249_) {
         super.setSelected(p_265249_);
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.nextEntry(ScreenDirection.UP);
         if (chatselectionscreen$chatselectionlist$entry == null) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }
      }

      @Override
      public boolean keyPressed(KeyEvent p_428157_) {
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.getSelected();
         return chatselectionscreen$chatselectionlist$entry != null && chatselectionscreen$chatselectionlist$entry.keyPressed(p_428157_)
            ? true
            : super.keyPressed(p_428157_);
      }

      public int getFooterTop() {
         return this.getBottom() + 9;
      }

      @OnlyIn(Dist.CLIENT)
      public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private final Component text;

         public DividerEntry(final Component p_239672_) {
            this.text = p_239672_;
         }

         @Override
         public void renderContent(GuiGraphics p_430082_, int p_423499_, int p_427428_, boolean p_424123_, float p_428447_) {
            int i = this.getContentYMiddle();
            int j = this.getContentRight() - 8;
            int k = ChatSelectionScreen.this.font.width(this.text);
            int l = (this.getContentX() + j - k) / 2;
            int i1 = i - 4;
            p_430082_.drawString(ChatSelectionScreen.this.font, this.text, l, i1, -6250336);
         }

         @Override
         public Component getNarration() {
            return this.text;
         }
      }

      @OnlyIn(Dist.CLIENT)
      public abstract static class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
         @Override
         public Component getNarration() {
            return CommonComponents.EMPTY;
         }

         public boolean isSelected() {
            return false;
         }

         public boolean canSelect() {
            return false;
         }

         public boolean canReport() {
            return this.canSelect();
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent p_428155_, boolean p_431246_) {
            return this.canSelect();
         }
      }

      @OnlyIn(Dist.CLIENT)
      record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
         public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading p_239748_) {
            return p_239748_.sender.equals(this.sender);
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int CHECKMARK_WIDTH = 9;
         private static final int CHECKMARK_HEIGHT = 8;
         private static final int INDENT_AMOUNT = 11;
         private static final int TAG_MARGIN_LEFT = 4;
         private final int chatId;
         private final FormattedText text;
         private final Component narration;
         private final @Nullable List<FormattedCharSequence> hoverText;
         private final GuiMessageTag.@Nullable Icon tagIcon;
         private final @Nullable List<FormattedCharSequence> tagHoverText;
         private final boolean canReport;
         private final boolean playerMessage;

         public MessageEntry(
            final int p_240650_,
            final Component p_240525_,
            final @Nullable Component p_240539_,
            final GuiMessageTag p_240551_,
            final boolean p_240596_,
            final boolean p_240615_
         ) {
            this.chatId = p_240650_;
            this.tagIcon = Optionull.map(p_240551_, GuiMessageTag::icon);
            this.tagHoverText = p_240551_ != null && p_240551_.text() != null
               ? ChatSelectionScreen.this.font.split(p_240551_.text(), ChatSelectionList.this.getRowWidth())
               : null;
            this.canReport = p_240596_;
            this.playerMessage = p_240615_;
            FormattedText formattedtext = ChatSelectionScreen.this.font
               .substrByWidth(p_240525_, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
            if (p_240525_ != formattedtext) {
               this.text = FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS);
               this.hoverText = ChatSelectionScreen.this.font.split(p_240525_, ChatSelectionList.this.getRowWidth());
            } else {
               this.text = p_240525_;
               this.hoverText = null;
            }

            this.narration = p_240539_;
         }

         @Override
         public void renderContent(GuiGraphics p_429798_, int p_425504_, int p_429315_, boolean p_431307_, float p_428581_) {
            if (this.isSelected() && this.canReport) {
               this.renderSelectedCheckmark(p_429798_, this.getContentY(), this.getContentX(), this.getContentHeight());
            }

            int i = this.getContentX() + this.getTextIndent();
            int j = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
            p_429798_.drawString(ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), i, j, this.canReport ? -1 : -1593835521);
            if (this.hoverText != null && p_431307_) {
               p_429798_.setTooltipForNextFrame(this.hoverText, p_425504_, p_429315_);
            }

            int k = ChatSelectionScreen.this.font.width(this.text);
            this.renderTag(p_429798_, i + k + 4, this.getContentY(), this.getContentHeight(), p_425504_, p_429315_);
         }

         private void renderTag(GuiGraphics p_281776_, int p_240566_, int p_240565_, int p_240581_, int p_240614_, int p_240612_) {
            if (this.tagIcon != null) {
               int i = p_240565_ + (p_240581_ - this.tagIcon.height) / 2;
               this.tagIcon.draw(p_281776_, p_240566_, i);
               if (this.tagHoverText != null
                  && p_240614_ >= p_240566_
                  && p_240614_ <= p_240566_ + this.tagIcon.width
                  && p_240612_ >= i
                  && p_240612_ <= i + this.tagIcon.height) {
                  p_281776_.setTooltipForNextFrame(this.tagHoverText, p_240614_, p_240612_);
               }
            }
         }

         private void renderSelectedCheckmark(GuiGraphics p_281342_, int p_281492_, int p_283046_, int p_283458_) {
            int i = p_281492_ + (p_283458_ - 8) / 2;
            p_281342_.blitSprite(RenderPipelines.GUI_TEXTURED, ChatSelectionScreen.CHECKMARK_SPRITE, p_283046_, i, 9, 8);
         }

         private int getMaximumTextWidth() {
            int i = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
            return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - i;
         }

         private int getTextIndent() {
            return this.playerMessage ? 11 : 0;
         }

         @Override
         public Component getNarration() {
            return this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration;
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent p_427878_, boolean p_429438_) {
            ChatSelectionList.this.setSelected((ChatSelectionScreen.ChatSelectionList.Entry)null);
            return this.toggleReport();
         }

         @Override
         public boolean keyPressed(KeyEvent p_426296_) {
            return p_426296_.isSelection() ? this.toggleReport() : false;
         }

         @Override
         public boolean isSelected() {
            return ChatSelectionScreen.this.report.isReported(this.chatId);
         }

         @Override
         public boolean canSelect() {
            return true;
         }

         @Override
         public boolean canReport() {
            return this.canReport;
         }

         private boolean toggleReport() {
            if (this.canReport) {
               ChatSelectionScreen.this.report.toggleReported(this.chatId);
               ChatSelectionScreen.this.updateConfirmSelectedButton();
               return true;
            } else {
               return false;
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int FACE_SIZE = 12;
         private static final int PADDING = 4;
         private final Component heading;
         private final Supplier<PlayerSkin> skin;
         private final boolean canReport;

         public MessageHeadingEntry(final GameProfile p_240080_, final Component p_240081_, final boolean p_240082_) {
            this.heading = p_240081_;
            this.canReport = p_240082_;
            this.skin = ChatSelectionList.this.minecraft.getSkinManager().createLookup(p_240080_, true);
         }

         @Override
         public void renderContent(GuiGraphics p_428272_, int p_424423_, int p_428020_, boolean p_431551_, float p_425981_) {
            int i = this.getContentX() - 12 + 4;
            int j = this.getContentY() + (this.getContentHeight() - 12) / 2;
            PlayerFaceRenderer.draw(p_428272_, this.skin.get(), i, j, 12);
            int k = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
            p_428272_.drawString(ChatSelectionScreen.this.font, this.heading, i + 12 + 4, k, this.canReport ? -1 : -1593835521);
         }
      }

      @OnlyIn(Dist.CLIENT)
      public static class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         @Override
         public void renderContent(GuiGraphics p_282007_, int p_240110_, int p_240111_, boolean p_240117_, float p_240118_) {
         }
      }
   }
}
