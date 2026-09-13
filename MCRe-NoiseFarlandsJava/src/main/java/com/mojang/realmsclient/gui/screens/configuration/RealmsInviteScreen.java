package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsInviteScreen extends RealmsScreen {
    private static final Component TITLE = Component.translatable("mco.configure.world.buttons.invite");
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.invite.profile.name").withColor(-6250336);
    private static final Component INVITING_PLAYER_TEXT = Component.translatable("mco.configure.world.players.inviting").withColor(-6250336);
    private static final Component NO_SUCH_PLAYER_ERROR_TEXT = Component.translatable("mco.configure.world.players.error").withColor(-65536);
    private static final Component DUPLICATE_PLAYER_TEXT = Component.translatable("mco.configure.world.players.invite.duplicate").withColor(-65536);
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable EditBox profileName;
    private @Nullable Button inviteButton;
    private final RealmsServer serverData;
    private final RealmsConfigureWorldScreen configureScreen;
    private @Nullable Component message;

    public RealmsInviteScreen(final RealmsConfigureWorldScreen configureScreen, final RealmsServer serverData) {
        super(TITLE);
        this.configureScreen = configureScreen;
        this.serverData = serverData;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        this.profileName = new EditBox(this.minecraft.font, 200, 20, Component.translatable("mco.configure.world.invite.profile.name"));
        content.addChild(CommonLayouts.labeledElement(this.font, this.profileName, NAME_LABEL));
        this.inviteButton = content.addChild(Button.builder(TITLE, button -> this.onInvite()).width(200).build());
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).width(200).build());
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    protected void setInitialFocus() {
        if (this.profileName != null) {
            this.setInitialFocus(this.profileName);
        }
    }

    private void onInvite() {
        if (this.inviteButton != null && this.profileName != null) {
            if (StringUtil.isBlank(this.profileName.getValue())) {
                this.showMessage(NO_SUCH_PLAYER_ERROR_TEXT);
            } else if (this.serverData.players.stream().anyMatch(player -> player.name.equalsIgnoreCase(this.profileName.getValue()))) {
                this.showMessage(DUPLICATE_PLAYER_TEXT);
            } else {
                long serverId = this.serverData.id;
                String name = this.profileName.getValue().trim();
                this.inviteButton.active = false;
                this.profileName.setEditable(false);
                this.showMessage(INVITING_PLAYER_TEXT);
                CompletableFuture.<Boolean>supplyAsync(() -> this.configureScreen.invitePlayer(serverId, name), Util.ioPool()).thenAcceptAsync(success -> {
                    if (success) {
                        this.minecraft.gui.setScreen(this.configureScreen);
                    } else {
                        this.showMessage(NO_SUCH_PLAYER_ERROR_TEXT);
                    }

                    this.profileName.setEditable(true);
                    this.inviteButton.active = true;
                }, this.screenExecutor);
            }
        }
    }

    private void showMessage(final Component message) {
        this.message = message;
        this.minecraft.getNarrator().saySystemNow(message);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.configureScreen);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        if (this.message != null && this.inviteButton != null) {
            graphics.centeredText(this.font, this.message, this.width / 2, this.inviteButton.getY() + this.inviteButton.getHeight() + 8, -1);
        }
    }
}