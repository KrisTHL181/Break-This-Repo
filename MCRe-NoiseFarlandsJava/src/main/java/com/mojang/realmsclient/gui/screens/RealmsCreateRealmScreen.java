package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsCreateRealmScreen extends RealmsScreen {
    private static final Component CREATE_REALM_TEXT = Component.translatable("mco.selectServer.create");
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
    private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
    private static final int BUTTON_SPACING = 10;
    private static final int CONTENT_WIDTH = 210;
    private final RealmsMainScreen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private EditBox nameBox;
    private EditBox descriptionBox;
    private final Runnable createWorldRunnable;

    public RealmsCreateRealmScreen(final RealmsMainScreen lastScreen, final RealmsServer server, final boolean isSnapshot) {
        super(CREATE_REALM_TEXT);
        this.lastScreen = lastScreen;
        this.createWorldRunnable = () -> this.createWorld(server, isSnapshot);
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical()).spacing(10);
        Button createButton = Button.builder(CommonComponents.GUI_CONTINUE, button -> this.createWorldRunnable.run()).build();
        createButton.active = false;
        this.nameBox = new EditBox(this.font, 210, 20, NAME_LABEL);
        this.nameBox.setResponder(value -> createButton.active = !StringUtil.isBlank(value));
        this.descriptionBox = new EditBox(this.font, 210, 20, DESCRIPTION_LABEL);
        content.addChild(CommonLayouts.labeledElement(this.font, this.nameBox, NAME_LABEL));
        content.addChild(CommonLayouts.labeledElement(this.font, this.descriptionBox, DESCRIPTION_LABEL));
        LinearLayout bottomButtons = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
        bottomButtons.addChild(createButton);
        bottomButtons.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.nameBox);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    private void createWorld(final RealmsServer server, final boolean initializeSnapshotRealm) {
        if (!server.isSnapshotRealm() && initializeSnapshotRealm) {
            AtomicBoolean canceled = new AtomicBoolean();
            this.minecraft.gui.setScreen(new AlertScreen(() -> {
                canceled.set(true);
                this.lastScreen.resetScreen();
                this.minecraft.gui.setScreen(this.lastScreen);
            }, Component.translatable("mco.upload.preparing"), Component.empty()));
            CompletableFuture.<RealmsServer>supplyAsync(() -> createSnapshotRealm(server), Util.backgroundExecutor()).thenAcceptAsync(snapshotServer -> {
                if (!canceled.get()) {
                    this.showResetWorldScreen(snapshotServer);
                }
            }, this.minecraft).exceptionallyAsync(ex -> {
                this.lastScreen.resetScreen();
                Component errorMessage;
                if (ex.getCause() instanceof RealmsServiceException realmsServiceException) {
                    errorMessage = realmsServiceException.realmsError.errorMessage();
                } else {
                    errorMessage = Component.translatable("mco.errorMessage.initialize.failed");
                }

                this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(errorMessage, this.lastScreen));
                return null;
            }, this.minecraft);
        } else {
            this.showResetWorldScreen(server);
        }
    }

    private static RealmsServer createSnapshotRealm(final RealmsServer server) {
        RealmsClient client = RealmsClient.getOrCreate();

        try {
            return client.createSnapshotRealm(server.id);
        } catch (RealmsServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void showResetWorldScreen(final RealmsServer server) {
        RealmCreationTask realmCreationTask = new RealmCreationTask(server.id, this.nameBox.getValue(), this.descriptionBox.getValue());
        RealmsResetWorldScreen resetWorldScreen = RealmsResetWorldScreen.forNewRealm(this, server, realmCreationTask, () -> this.minecraft.execute(() -> {
            RealmsMainScreen.refreshServerList();
            this.minecraft.gui.setScreen(this.lastScreen);
        }));
        this.minecraft.gui.setScreen(resetWorldScreen);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}