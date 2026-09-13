package net.minecraft.client.gui.screens.reporting;

import java.util.function.Consumer;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import org.jspecify.annotations.Nullable;

public class ReportReasonSelectionScreen extends Screen {
   private static final Component REASON_TITLE = Component.translatable("gui.abuseReport.reason.title");
   private static final Component REASON_DESCRIPTION = Component.translatable("gui.abuseReport.reason.description");
   private static final Component READ_INFO_LABEL = Component.translatable("gui.abuseReport.read_info");
   private static final int DESCRIPTION_BOX_WIDTH = 320;
   private static final int DESCRIPTION_BOX_HEIGHT = 62;
   private static final int PADDING = 4;
   private final @Nullable Screen lastScreen;
   private ReportReasonSelectionScreen.@Nullable ReasonSelectionList reasonSelectionList;
   private @Nullable ReportReason currentlySelectedReason;
   private final Consumer<ReportReason> onSelectedReason;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final ReportType reportType;

   public ReportReasonSelectionScreen(
      final @Nullable Screen lastScreen,
      final @Nullable ReportReason selectedReason,
      final ReportType reportType,
      final Consumer<ReportReason> onSelectedReason
   ) {
      super(REASON_TITLE);
      this.lastScreen = lastScreen;
      this.currentlySelectedReason = selectedReason;
      this.onSelectedReason = onSelectedReason;
      this.reportType = reportType;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(REASON_TITLE, this.font);
      LinearLayout content = this.layout.addToContents(LinearLayout.vertical().spacing(4));
      this.reasonSelectionList = content.addChild(new ReportReasonSelectionScreen.ReasonSelectionList(this.minecraft));
      ReportReasonSelectionScreen.ReasonSelectionList.Entry selectedEntry = Optionull.map(this.currentlySelectedReason, this.reasonSelectionList::findEntry);
      this.reasonSelectionList.setSelected(selectedEntry);
      content.addChild(SpacerElement.height(this.descriptionHeight()));
      LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      footer.addChild(Button.builder(READ_INFO_LABEL, ConfirmLinkScreen.confirmLink(this, CommonLinks.REPORTING_HELP)).build());
      footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
         ReportReasonSelectionScreen.ReasonSelectionList.Entry selected = this.reasonSelectionList.getSelected();
         if (selected != null) {
            this.onSelectedReason.accept(selected.getReason());
         }

         this.minecraft.gui.setScreen(this.lastScreen);
      }).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.reasonSelectionList != null) {
         this.reasonSelectionList.updateSizeAndPosition(this.width, this.listHeight(), this.layout.getHeaderHeight());
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.fill(this.descriptionLeft(), this.descriptionTop(), this.descriptionRight(), this.descriptionBottom(), -16777216);
      graphics.outline(this.descriptionLeft(), this.descriptionTop(), this.descriptionWidth(), this.descriptionHeight(), -1);
      graphics.text(this.font, REASON_DESCRIPTION, this.descriptionLeft() + 4, this.descriptionTop() + 4, -1);
      ReportReasonSelectionScreen.ReasonSelectionList.Entry selectedEntry = this.reasonSelectionList.getSelected();
      if (selectedEntry != null) {
         int textLeft = this.descriptionLeft() + 4 + 16;
         int textRight = this.descriptionRight() - 4;
         int textTop = this.descriptionTop() + 4 + 9 + 2;
         int textBottom = this.descriptionBottom() - 4;
         int textWidth = textRight - textLeft;
         int textHeight = textBottom - textTop;
         int contentHeight = this.font.wordWrapHeight(selectedEntry.reason.description(), textWidth);
         graphics.textWithWordWrap(this.font, selectedEntry.reason.description(), textLeft, textTop + (textHeight - contentHeight) / 2, textWidth, -1);
      }
   }

   private int descriptionLeft() {
      return (this.width - 320) / 2;
   }

   private int descriptionRight() {
      return (this.width + 320) / 2;
   }

   private int descriptionTop() {
      return this.descriptionBottom() - this.descriptionHeight();
   }

   private int descriptionBottom() {
      return this.height - this.layout.getFooterHeight() - 4;
   }

   private int descriptionWidth() {
      return 320;
   }

   private int descriptionHeight() {
      return 62;
   }

   private int listHeight() {
      return this.layout.getContentHeight() - this.descriptionHeight() - 8;
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   public class ReasonSelectionList extends ObjectSelectionList<ReportReasonSelectionScreen.ReasonSelectionList.Entry> {
      public ReasonSelectionList(final Minecraft minecraft) {
         super(
            minecraft,
            ReportReasonSelectionScreen.this.width,
            ReportReasonSelectionScreen.this.listHeight(),
            ReportReasonSelectionScreen.this.layout.getHeaderHeight(),
            18
         );

         for (ReportReason reason : ReportReason.values()) {
            if (!ReportReason.getIncompatibleCategories(ReportReasonSelectionScreen.this.reportType).contains(reason)) {
               this.addEntry(new ReportReasonSelectionScreen.ReasonSelectionList.Entry(reason));
            }
         }
      }

      public ReportReasonSelectionScreen.ReasonSelectionList.@Nullable Entry findEntry(final ReportReason reason) {
         return this.children().stream().filter(entry -> entry.reason == reason).findFirst().orElse(null);
      }

      @Override
      public int getRowWidth() {
         return 320;
      }

      public void setSelected(final ReportReasonSelectionScreen.ReasonSelectionList.@Nullable Entry selected) {
         super.setSelected(selected);
         ReportReasonSelectionScreen.this.currentlySelectedReason = selected != null ? selected.getReason() : null;
      }

      public class Entry extends ObjectSelectionList.Entry<ReportReasonSelectionScreen.ReasonSelectionList.Entry> {
         private final ReportReason reason;

         public Entry(final ReportReason reason) {
            this.reason = reason;
         }

         @Override
         public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            int textX = this.getContentX() + 1;
            int textY = this.getContentY() + (this.getContentHeight() - 9) / 2 + 1;
            graphics.text(ReportReasonSelectionScreen.this.font, this.reason.title(), textX, textY, -1);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("gui.abuseReport.reason.narration", this.reason.title(), this.reason.description());
         }

         @Override
         public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            ReasonSelectionList.this.setSelected(this);
            return super.mouseClicked(event, doubleClick);
         }

         public ReportReason getReason() {
            return this.reason;
         }
      }
   }
}
