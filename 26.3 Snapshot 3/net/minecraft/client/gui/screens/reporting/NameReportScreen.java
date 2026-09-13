package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
   private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
   private static final Component COMMENT_BOX_LABEL = Component.translatable("gui.abuseReport.name.comment_box_label");
   private @Nullable MultiLineEditBox commentBox;

   private NameReportScreen(final Screen lastScreen, final ReportingContext reportingContext, final NameReport.Builder reportBuilder) {
      super(TITLE, lastScreen, reportingContext, reportBuilder);
   }

   public NameReportScreen(final Screen lastScreen, final ReportingContext reportingContext, final UUID playerId, final String reportedName) {
      this(lastScreen, reportingContext, new NameReport.Builder(playerId, reportedName, reportingContext.sender().reportLimits()));
   }

   public NameReportScreen(final Screen lastScreen, final ReportingContext reportingContext, final NameReport draft) {
      this(lastScreen, reportingContext, new NameReport.Builder(draft, reportingContext.sender().reportLimits()));
   }

   @Override
   protected void addContent() {
      Component reportedName = Component.literal(this.reportBuilder.report().getReportedName()).withStyle(ChatFormatting.YELLOW);
      this.layout
         .addChild(
            new StringWidget(Component.translatable("gui.abuseReport.name.reporting", reportedName), this.font), s -> s.alignHorizontallyCenter().padding(0, 8)
         );
      this.commentBox = this.createCommentBox(280, 9 * 8, comments -> {
         this.reportBuilder.setComments(comments);
         this.onReportChanged();
      });
      this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, COMMENT_BOX_LABEL, s -> s.paddingBottom(12)));
   }

   @Override
   public boolean mouseReleased(final MouseButtonEvent event) {
      if (super.mouseReleased(event)) {
         return true;
      } else {
         return this.commentBox != null ? this.commentBox.mouseReleased(event) : false;
      }
   }
}
