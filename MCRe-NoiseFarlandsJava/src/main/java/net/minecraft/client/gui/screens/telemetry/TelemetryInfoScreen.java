package net.minecraft.client.gui.screens.telemetry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TelemetryInfoScreen extends Screen {
    private static final Component TITLE = Component.translatable("telemetry_info.screen.title");
    private static final Component DESCRIPTION = Component.translatable("telemetry_info.screen.description").withColor(-4539718);
    private static final Component BUTTON_PRIVACY_STATEMENT = Component.translatable("telemetry_info.button.privacy_statement");
    private static final Component BUTTON_GIVE_FEEDBACK = Component.translatable("telemetry_info.button.give_feedback");
    private static final Component BUTTON_VIEW_DATA = Component.translatable("telemetry_info.button.show_data");
    private static final Component CHECKBOX_OPT_IN = Component.translatable("telemetry_info.opt_in.description").withColor(-2039584);
    private static final int SPACING = 8;
    private static final boolean EXTRA_TELEMETRY_AVAILABLE = Minecraft.getInstance().extraTelemetryAvailable();
    private final Screen lastScreen;
    private final Options options;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(
        this, 16 + 9 * 5 + 20, EXTRA_TELEMETRY_AVAILABLE ? 33 + Checkbox.getBoxSize(Minecraft.getInstance().font) : 33
    );
    private @Nullable TelemetryEventWidget telemetryEventWidget;
    private @Nullable MultiLineTextWidget description;
    private @Nullable Checkbox checkbox;
    private double savedScroll;

    public TelemetryInfoScreen(final Screen lastScreen, final Options options) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.options = options;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), DESCRIPTION);
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(TITLE, this.font));
        this.description = header.addChild(new MultiLineTextWidget(DESCRIPTION, this.font).setCentered(true));
        LinearLayout upperContentButtons = header.addChild(LinearLayout.horizontal().spacing(8));
        upperContentButtons.addChild(Button.builder(BUTTON_PRIVACY_STATEMENT, this::openPrivacyStatementLink).build());
        upperContentButtons.addChild(Button.builder(BUTTON_GIVE_FEEDBACK, this::openFeedbackLink).build());
        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
        footer.defaultCellSetting().alignHorizontallyCenter();
        if (EXTRA_TELEMETRY_AVAILABLE) {
            this.checkbox = footer.addChild(
                Checkbox.builder(CHECKBOX_OPT_IN, this.font)
                    .maxWidth(this.width - 40)
                    .selected(this.options.telemetryOptInExtra())
                    .onValueChange(this::onOptInChanged)
                    .build()
            );
        }

        LinearLayout footerButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        footerButtons.addChild(Button.builder(BUTTON_VIEW_DATA, this::openDataFolder).build());
        footerButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).build());
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        this.telemetryEventWidget = content.addChild(new TelemetryEventWidget(0, 0, this.width - 40, this.layout.getContentHeight(), this.font));
        this.telemetryEventWidget.setOnScrolledListener(scroll -> this.savedScroll = scroll);
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.telemetryEventWidget != null) {
            this.telemetryEventWidget.setScrollAmount(this.savedScroll);
            this.telemetryEventWidget.setWidth(this.width - 40);
            this.telemetryEventWidget.setHeight(this.layout.getContentHeight());
            this.telemetryEventWidget.updateLayout();
        }

        if (this.description != null) {
            this.description.setMaxWidth(this.width - 16);
        }

        if (this.checkbox != null) {
            this.checkbox.adjustWidth(this.width - 40, this.font);
        }

        this.layout.arrangeElements();
    }

    @Override
    protected void setInitialFocus() {
        if (this.telemetryEventWidget != null) {
            this.setInitialFocus(this.telemetryEventWidget);
        }
    }

    private void onOptInChanged(final AbstractWidget widget, final boolean value) {
        if (this.telemetryEventWidget != null) {
            this.telemetryEventWidget.onOptInChanged(value);
        }
    }

    private void openPrivacyStatementLink(final Button button) {
        ConfirmLinkScreen.confirmLinkNow(this, CommonLinks.PRIVACY_STATEMENT);
    }

    private void openFeedbackLink(final Button button) {
        ConfirmLinkScreen.confirmLinkNow(this, CommonLinks.RELEASE_FEEDBACK);
    }

    private void openDataFolder(final Button button) {
        Util.getPlatform().openPath(this.minecraft.getTelemetryManager().getLogDirectory());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}