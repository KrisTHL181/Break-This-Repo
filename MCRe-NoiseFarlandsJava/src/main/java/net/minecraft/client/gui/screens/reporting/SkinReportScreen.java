package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.multiplayer.chat.report.SkinReport;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinReportScreen extends AbstractReportScreen<SkinReport.Builder> {
    private static final int SKIN_WIDTH = 85;
    private static final int FORM_WIDTH = 178;
    private static final Component TITLE = Component.translatable("gui.abuseReport.skin.title");
    private MultiLineEditBox commentBox;
    private Button selectReasonButton;

    private SkinReportScreen(final Screen lastScreen, final ReportingContext reportingContext, final SkinReport.Builder reportBuilder) {
        super(TITLE, lastScreen, reportingContext, reportBuilder);
    }

    public SkinReportScreen(final Screen lastScreen, final ReportingContext reportingContext, final UUID playerId, final Supplier<PlayerSkin> skinGetter) {
        this(lastScreen, reportingContext, new SkinReport.Builder(playerId, skinGetter, reportingContext.sender().reportLimits()));
    }

    public SkinReportScreen(final Screen lastScreen, final ReportingContext reportingContext, final SkinReport draft) {
        this(lastScreen, reportingContext, new SkinReport.Builder(draft, reportingContext.sender().reportLimits()));
    }

    @Override
    protected void addContent() {
        LinearLayout contentLayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        contentLayout.defaultCellSetting().alignVerticallyMiddle();
        contentLayout.addChild(new PlayerSkinWidget(85, 120, this.minecraft.getEntityModels(), this.reportBuilder.report().getSkinGetter()));
        LinearLayout formLayout = contentLayout.addChild(LinearLayout.vertical().spacing(8));
        this.selectReasonButton = Button.builder(
                SELECT_REASON,
                var1x -> this.minecraft.gui.setScreen(new ReportReasonSelectionScreen(this, this.reportBuilder.reason(), ReportType.SKIN, reason -> {
                    this.reportBuilder.setReason(reason);
                    this.onReportChanged();
                }))
            )
            .width(178)
            .build();
        formLayout.addChild(CommonLayouts.labeledElement(this.font, this.selectReasonButton, OBSERVED_WHAT_LABEL));
        this.commentBox = this.createCommentBox(178, 9 * 8, comments -> {
            this.reportBuilder.setComments(comments);
            this.onReportChanged();
        });
        formLayout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, s -> s.paddingBottom(12)));
    }

    @Override
    protected void onReportChanged() {
        ReportReason reportReason = this.reportBuilder.reason();
        if (reportReason != null) {
            this.selectReasonButton.setMessage(reportReason.title());
        } else {
            this.selectReasonButton.setMessage(SELECT_REASON);
        }

        super.onReportChanged();
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        return super.mouseReleased(event) ? true : this.commentBox.mouseReleased(event);
    }
}