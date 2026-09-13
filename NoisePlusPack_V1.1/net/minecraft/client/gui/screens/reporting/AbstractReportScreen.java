package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractReportScreen<B extends Report.Builder<?>> extends Screen {
   private static final Component REPORT_SENT_MESSAGE = Component.translatable("gui.abuseReport.report_sent_msg");
   private static final Component REPORT_SENDING_TITLE = Component.translatable("gui.abuseReport.sending.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_SENT_TITLE = Component.translatable("gui.abuseReport.sent.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_ERROR_TITLE = Component.translatable("gui.abuseReport.error.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_SEND_GENERIC_ERROR = Component.translatable("gui.abuseReport.send.generic_error");
   protected static final Component SEND_REPORT = Component.translatable("gui.abuseReport.send");
   protected static final Component OBSERVED_WHAT_LABEL = Component.translatable("gui.abuseReport.observed_what");
   protected static final Component SELECT_REASON = Component.translatable("gui.abuseReport.select_reason");
   private static final Component DESCRIBE_PLACEHOLDER = Component.translatable("gui.abuseReport.describe");
   protected static final Component MORE_COMMENTS_LABEL = Component.translatable("gui.abuseReport.more_comments");
   private static final Component MORE_COMMENTS_NARRATION = Component.translatable("gui.abuseReport.comments");
   private static final Component ATTESTATION_CHECKBOX = Component.translatable("gui.abuseReport.attestation").withColor(-2039584);
   protected static final int BUTTON_WIDTH = 120;
   protected static final int MARGIN = 20;
   protected static final int SCREEN_WIDTH = 280;
   protected static final int SPACING = 8;
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Screen lastScreen;
   protected final ReportingContext reportingContext;
   protected final LinearLayout layout = LinearLayout.vertical().spacing(8);
   protected B reportBuilder;
   private Checkbox attestation;
   protected Button sendButton;

   protected AbstractReportScreen(Component p_297559_, Screen p_299592_, ReportingContext p_300174_, B p_300351_) {
      super(p_297559_);
      this.lastScreen = p_299592_;
      this.reportingContext = p_300174_;
      this.reportBuilder = p_300351_;
   }

   protected MultiLineEditBox createCommentBox(int p_297252_, int p_301025_, Consumer<String> p_298469_) {
      AbuseReportLimits abusereportlimits = this.reportingContext.sender().reportLimits();
      MultiLineEditBox multilineeditbox = MultiLineEditBox.builder()
         .setPlaceholder(DESCRIBE_PLACEHOLDER)
         .build(this.font, p_297252_, p_301025_, MORE_COMMENTS_NARRATION);
      multilineeditbox.setValue(this.reportBuilder.comments());
      multilineeditbox.setCharacterLimit(abusereportlimits.maxOpinionCommentsLength());
      multilineeditbox.setValueListener(p_298469_);
      return multilineeditbox;
   }

   @Override
   protected void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.createHeader();
      this.addContent();
      this.createFooter();
      this.onReportChanged();
      this.layout.visitWidgets(p_340819_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_340819_);
      });
      this.repositionElements();
   }

   protected void createHeader() {
      this.layout.addChild(new StringWidget(this.title, this.font));
   }

   protected abstract void addContent();

   protected void createFooter() {
      this.attestation = this.layout
         .addChild(
            Checkbox.builder(ATTESTATION_CHECKBOX, this.font).selected(this.reportBuilder.attested()).maxWidth(280).onValueChange((p_340816_, p_340817_) -> {
               this.reportBuilder.setAttested(p_340817_);
               this.onReportChanged();
            }).build()
         );
      LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
      linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_340815_ -> this.onClose()).width(120).build());
      this.sendButton = linearlayout.addChild(Button.builder(SEND_REPORT, p_340820_ -> this.sendReport()).width(120).build());
   }

   protected void onReportChanged() {
      Report.CannotBuildReason report$cannotbuildreason = this.reportBuilder.checkBuildable();
      this.sendButton.active = report$cannotbuildreason == null && this.attestation.selected();
      this.sendButton.setTooltip(Optionull.map(report$cannotbuildreason, Report.CannotBuildReason::tooltip));
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   protected void sendReport() {
      this.reportBuilder.build(this.reportingContext).ifLeft(p_301124_ -> {
         CompletableFuture<?> completablefuture = this.reportingContext.sender().send(p_301124_.id(), p_301124_.reportType(), p_301124_.report());
         this.minecraft.setScreen(GenericWaitingScreen.createWaiting(REPORT_SENDING_TITLE, CommonComponents.GUI_CANCEL, () -> {
            this.minecraft.setScreen(this);
            completablefuture.cancel(true);
         }));
         completablefuture.handleAsync((p_301251_, p_299485_) -> {
            if (p_299485_ == null) {
               this.onReportSendSuccess();
            } else {
               if (p_299485_ instanceof CancellationException) {
                  return null;
               }

               this.onReportSendError(p_299485_);
            }

            return null;
         }, this.minecraft);
      }).ifRight(p_298848_ -> this.displayReportSendError(p_298848_.message()));
   }

   private void onReportSendSuccess() {
      this.clearDraft();
      this.minecraft
         .setScreen(
            GenericWaitingScreen.createCompleted(REPORT_SENT_TITLE, REPORT_SENT_MESSAGE, CommonComponents.GUI_DONE, () -> this.minecraft.setScreen(null))
         );
   }

   private void onReportSendError(Throwable p_297880_) {
      LOGGER.error("Encountered error while sending abuse report", p_297880_);
      Component component;
      if (p_297880_.getCause() instanceof ThrowingComponent throwingcomponent) {
         component = throwingcomponent.getComponent();
      } else {
         component = REPORT_SEND_GENERIC_ERROR;
      }

      this.displayReportSendError(component);
   }

   private void displayReportSendError(Component p_301245_) {
      Component component = p_301245_.copy().withStyle(ChatFormatting.RED);
      this.minecraft
         .setScreen(GenericWaitingScreen.createCompleted(REPORT_ERROR_TITLE, component, CommonComponents.GUI_BACK, () -> this.minecraft.setScreen(this)));
   }

   void saveDraft() {
      if (this.reportBuilder.hasContent()) {
         this.reportingContext.setReportDraft(this.reportBuilder.report().copy());
      }
   }

   void clearDraft() {
      this.reportingContext.setReportDraft(null);
   }

   @Override
   public void onClose() {
      if (this.reportBuilder.hasContent()) {
         this.minecraft.setScreen(new AbstractReportScreen.DiscardReportWarningScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }
   }

   @Override
   public void removed() {
      this.saveDraft();
      super.removed();
   }

   @OnlyIn(Dist.CLIENT)
   class DiscardReportWarningScreen extends WarningScreen {
      private static final Component TITLE = Component.translatable("gui.abuseReport.discard.title").withStyle(ChatFormatting.BOLD);
      private static final Component MESSAGE = Component.translatable("gui.abuseReport.discard.content");
      private static final Component RETURN = Component.translatable("gui.abuseReport.discard.return");
      private static final Component DRAFT = Component.translatable("gui.abuseReport.discard.draft");
      private static final Component DISCARD = Component.translatable("gui.abuseReport.discard.discard");

      protected DiscardReportWarningScreen() {
         super(TITLE, MESSAGE, MESSAGE);
      }

      @Override
      protected Layout addFooterButtons() {
         LinearLayout linearlayout = LinearLayout.vertical().spacing(8);
         linearlayout.defaultCellSetting().alignHorizontallyCenter();
         LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal().spacing(8));
         linearlayout1.addChild(Button.builder(RETURN, p_299113_ -> this.onClose()).build());
         linearlayout1.addChild(Button.builder(DRAFT, p_301082_ -> {
            AbstractReportScreen.this.saveDraft();
            this.minecraft.setScreen(AbstractReportScreen.this.lastScreen);
         }).build());
         linearlayout.addChild(Button.builder(DISCARD, p_299406_ -> {
            AbstractReportScreen.this.clearDraft();
            this.minecraft.setScreen(AbstractReportScreen.this.lastScreen);
         }).build());
         return linearlayout;
      }

      @Override
      public void onClose() {
         this.minecraft.setScreen(AbstractReportScreen.this);
      }

      @Override
      public boolean shouldCloseOnEsc() {
         return false;
      }
   }
}
