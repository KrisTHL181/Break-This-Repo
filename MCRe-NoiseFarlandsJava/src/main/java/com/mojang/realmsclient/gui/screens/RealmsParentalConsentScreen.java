package com.mojang.realmsclient.gui.screens;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsParentalConsentScreen extends RealmsScreen {
    private static final Component MESSAGE = Component.translatable("mco.account.privacy.information");
    private static final int SPACING = 15;
    private final LinearLayout layout = LinearLayout.vertical();
    private final Screen lastScreen;
    private @Nullable MultiLineTextWidget textWidget;

    public RealmsParentalConsentScreen(final Screen lastScreen) {
        super(GameNarrator.NO_TITLE);
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        this.layout.spacing(15).defaultCellSetting().alignHorizontallyCenter();
        this.textWidget = new MultiLineTextWidget(MESSAGE, this.font).setCentered(true);
        this.layout.addChild(this.textWidget);
        LinearLayout buttonLayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        Component privacyInfo = Component.translatable("mco.account.privacy.info.button");
        buttonLayout.addChild(Button.builder(privacyInfo, ConfirmLinkScreen.confirmLink(this, CommonLinks.GDPR)).build());
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Override
    protected void repositionElements() {
        if (this.textWidget != null) {
            this.textWidget.setMaxWidth(this.width - 15);
        }

        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public Component getNarrationMessage() {
        return MESSAGE;
    }
}