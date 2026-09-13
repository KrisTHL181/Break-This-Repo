package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPendingInvitesScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
    private final Screen lastScreen;
    private final CompletableFuture<List<PendingInvite>> pendingInvites = CompletableFuture.supplyAsync(() -> {
        try {
            return RealmsClient.getOrCreate().pendingInvites().pendingInvites();
        } catch (RealmsServiceException e) {
            LOGGER.error("Couldn't list invites", e);
            return List.of();
        }
    }, Util.nonCriticalIoPool());
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private RealmsPendingInvitesScreen.@Nullable PendingInvitationSelectionList pendingInvitationSelectionList;

    public RealmsPendingInvitesScreen(final Screen lastScreen, final Component title) {
        super(title);
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        RealmsMainScreen.refreshPendingInvites();
        this.layout.addTitleHeader(this.title, this.font);
        this.pendingInvitationSelectionList = this.layout.addToContents(new RealmsPendingInvitesScreen.PendingInvitationSelectionList(this.minecraft));
        this.pendingInvites.thenAcceptAsync(invites -> {
            List<RealmsPendingInvitesScreen.Entry> entries = invites.stream().map(x$0 -> new RealmsPendingInvitesScreen.Entry(x$0)).toList();
            this.pendingInvitationSelectionList.replaceEntries(entries);
            if (entries.isEmpty()) {
                this.minecraft.getNarrator().saySystemQueued(NO_PENDING_INVITES_TEXT);
            }
        }, this.screenExecutor);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.pendingInvitationSelectionList != null) {
            this.pendingInvitationSelectionList.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        if (this.pendingInvites.isDone() && this.pendingInvitationSelectionList.hasPendingInvites()) {
            graphics.centeredText(this.font, NO_PENDING_INVITES_TEXT, this.width / 2, this.height / 2 - 20, -1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class Entry extends ContainerObjectSelectionList.Entry<RealmsPendingInvitesScreen.Entry> {
        private static final Component ACCEPT_INVITE = Component.translatable("mco.invites.button.accept");
        private static final Component REJECT_INVITE = Component.translatable("mco.invites.button.reject");
        private static final WidgetSprites ACCEPT_SPRITE = new WidgetSprites(
            Identifier.withDefaultNamespace("pending_invite/accept"), Identifier.withDefaultNamespace("pending_invite/accept_highlighted")
        );
        private static final WidgetSprites REJECT_SPRITE = new WidgetSprites(
            Identifier.withDefaultNamespace("pending_invite/reject"), Identifier.withDefaultNamespace("pending_invite/reject_highlighted")
        );
        private static final int SPRITE_TEXTURE_SIZE = 18;
        private static final int SPRITE_SIZE = 21;
        private static final int TEXT_LEFT = 38;
        private final PendingInvite pendingInvite;
        private final List<AbstractWidget> children = new ArrayList<>();
        private final SpriteIconButton acceptButton;
        private final SpriteIconButton rejectButton;
        private final StringWidget realmName;
        private final StringWidget realmOwnerName;
        private final StringWidget inviteDate;

        public Entry(final PendingInvite pendingInvite) {
            this.pendingInvite = pendingInvite;
            int maxTextWidth = RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.getRowWidth() - 32 - 32 - 42;
            this.realmName = new StringWidget(Component.literal(pendingInvite.realmName()), RealmsPendingInvitesScreen.this.font).setMaxWidth(maxTextWidth);
            this.realmOwnerName = new StringWidget(Component.literal(pendingInvite.realmOwnerName()).withColor(-6250336), RealmsPendingInvitesScreen.this.font)
                .setMaxWidth(maxTextWidth);
            this.inviteDate = new StringWidget(
                    ComponentUtils.mergeStyles(RealmsUtil.convertToAgePresentationFromInstant(pendingInvite.date()), Style.EMPTY.withColor(-6250336)),
                    RealmsPendingInvitesScreen.this.font
                )
                .setMaxWidth(maxTextWidth);
            Button.CreateNarration narration = this.getCreateNarration(pendingInvite);
            this.acceptButton = SpriteIconButton.builder(ACCEPT_INVITE, button -> this.handleInvitation(true), false)
                .sprite(ACCEPT_SPRITE, 18, 18)
                .size(21, 21)
                .narration(narration)
                .withTootip()
                .build();
            this.rejectButton = SpriteIconButton.builder(REJECT_INVITE, button -> this.handleInvitation(false), false)
                .sprite(REJECT_SPRITE, 18, 18)
                .size(21, 21)
                .narration(narration)
                .withTootip()
                .build();
            this.children.addAll(List.of(this.acceptButton, this.rejectButton));
        }

        private Button.CreateNarration getCreateNarration(final PendingInvite pendingInvite) {
            return defaultNarrationSupplier -> {
                MutableComponent narration = CommonComponents.joinForNarration(
                    defaultNarrationSupplier.get(),
                    Component.literal(pendingInvite.realmName()),
                    Component.literal(pendingInvite.realmOwnerName()),
                    RealmsUtil.convertToAgePresentationFromInstant(pendingInvite.date())
                );
                return Component.translatable("narrator.select", narration);
            };
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            int x = this.getContentX();
            int y = this.getContentY();
            int textX = x + 38;
            RealmsUtil.extractPlayerFace(graphics, x, y, 32, this.pendingInvite.realmOwnerUuid());
            this.realmName.setPosition(textX, y + 1);
            this.realmName.extractWidgetRenderState(graphics, mouseX, mouseY, x);
            this.realmOwnerName.setPosition(textX, y + 12);
            this.realmOwnerName.extractWidgetRenderState(graphics, mouseX, mouseY, x);
            this.inviteDate.setPosition(textX, y + 24);
            this.inviteDate.extractWidgetRenderState(graphics, mouseX, mouseY, x);
            int buttonY = y + this.getContentHeight() / 2 - 10;
            this.acceptButton.setPosition(x + this.getContentWidth() - 16 - 42, buttonY);
            this.acceptButton.extractRenderState(graphics, mouseX, mouseY, a);
            this.rejectButton.setPosition(x + this.getContentWidth() - 8 - 21, buttonY);
            this.rejectButton.extractRenderState(graphics, mouseX, mouseY, a);
        }

        private void handleInvitation(final boolean accept) {
            String invitationId = this.pendingInvite.invitationId();
            CompletableFuture.<Boolean>supplyAsync(() -> {
                try {
                    RealmsClient client = RealmsClient.getOrCreate();
                    if (accept) {
                        client.acceptInvitation(invitationId);
                    } else {
                        client.rejectInvitation(invitationId);
                    }

                    return true;
                } catch (RealmsServiceException e) {
                    RealmsPendingInvitesScreen.LOGGER.error("Couldn't handle invite", e);
                    return false;
                }
            }, Util.ioPool()).thenAcceptAsync(result -> {
                if (result) {
                    RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.removeInvitation(this);
                    RealmsDataFetcher dataFetcher = RealmsPendingInvitesScreen.this.minecraft.realmsDataFetcher();
                    if (accept) {
                        dataFetcher.serverListUpdateTask.reset();
                    }

                    dataFetcher.pendingInvitesTask.reset();
                }
            }, RealmsPendingInvitesScreen.this.screenExecutor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class PendingInvitationSelectionList extends ContainerObjectSelectionList<RealmsPendingInvitesScreen.Entry> {
        public static final int ITEM_HEIGHT = 36;

        public PendingInvitationSelectionList(final Minecraft minecraft) {
            super(
                minecraft,
                RealmsPendingInvitesScreen.this.width,
                RealmsPendingInvitesScreen.this.layout.getContentHeight(),
                RealmsPendingInvitesScreen.this.layout.getHeaderHeight(),
                36
            );
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        public boolean hasPendingInvites() {
            return this.getItemCount() == 0;
        }

        public void removeInvitation(final RealmsPendingInvitesScreen.Entry entry) {
            this.removeEntry(entry);
        }
    }
}