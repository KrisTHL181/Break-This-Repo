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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
   private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
   private static final Component COMMENT_BOX_LABEL = Component.translatable("gui.abuseReport.name.comment_box_label");
   private @Nullable MultiLineEditBox commentBox;

   private NameReportScreen(Screen p_300534_, ReportingContext p_300915_, NameReport.Builder p_300014_) {
      super(TITLE, p_300534_, p_300915_, p_300014_);
   }

   public NameReportScreen(Screen p_300152_, ReportingContext p_300083_, UUID p_298096_, String p_300249_) {
      this(p_300152_, p_300083_, new NameReport.Builder(p_298096_, p_300249_, p_300083_.sender().reportLimits()));
   }

   public NameReportScreen(Screen p_300445_, ReportingContext p_299367_, NameReport p_297896_) {
      this(p_300445_, p_299367_, new NameReport.Builder(p_297896_, p_299367_.sender().reportLimits()));
   }

   @Override
   protected void addContent() {
      Component component = Component.literal(this.reportBuilder.report().getReportedName()).withStyle(ChatFormatting.YELLOW);
      this.layout
         .addChild(
            new StringWidget(Component.translatable("gui.abuseReport.name.reporting", component), this.font),
            p_357694_ -> p_357694_.alignHorizontallyCenter().padding(0, 8)
         );
      this.commentBox = this.createCommentBox(280, 72, p_420775_ -> {
         this.reportBuilder.setComments(p_420775_);
         this.onReportChanged();
      });
      this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, COMMENT_BOX_LABEL, p_299823_ -> p_299823_.paddingBottom(12)));
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_431688_) {
      if (super.mouseReleased(p_431688_)) {
         return true;
      } else {
         return this.commentBox != null ? this.commentBox.mouseReleased(p_431688_) : false;
      }
   }
}
