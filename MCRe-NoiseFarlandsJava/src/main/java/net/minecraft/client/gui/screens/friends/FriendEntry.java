package net.minecraft.client.gui.screens.friends;

import com.mojang.authlib.yggdrasil.response.PresenceStatus;
import com.mojang.authlib.yggdrasil.response.PresenceStatusDto;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.toasts.FriendToast;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.p2p.FriendJoinHandler;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
class FriendEntry extends AbstractFriendsEntryContainerWidget {
    private static final WidgetSprites REMOVE_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/remove"));
    private static final WidgetSprites INVITE_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/multiplayer/invite"));
    private static final WidgetSprites JOIN_REQUEST_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/multiplayer/join_request"));
    private static final WidgetSprites ACCEPT_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/accept"));
    private static final WidgetSprites REJECT_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/reject"));
    private static final Component UNFRIEND = Component.translatable("gui.friends.unfriend");
    private static final Component INVITE = Component.translatable("gui.friends.multiplayer.invite");
    private static final Component ACCEPT_INVITE = Component.translatable("gui.friends.multiplayer.invite_accept");
    private static final Component REJECT_INVITE = Component.translatable("gui.friends.multiplayer.invite_reject");
    private static final Component JOIN_REQUEST = Component.translatable("gui.friends.multiplayer.join_request");
    private static final Component ACCEPT_JOIN_REQUEST = Component.translatable("gui.friends.multiplayer.join_request_accept");
    private static final Component REJECT_JOIN_REQUEST = Component.translatable("gui.friends.multiplayer.join_request_reject");
    private static final Component CONFIRM_TITLE = Component.translatable("gui.friends.confirm_title");
    private static final Component CONFIRM_UNFRIEND = Component.translatable("gui.friends.confirm_unfriend");
    private static final Tooltip PENDING_JOIN_REQUEST_TOOLTIP = Tooltip.create(Component.translatable("gui.friends.button.loading.join_request_pending"));
    private static final Tooltip PENDING_INVITE_REQUEST_TOOLTIP = Tooltip.create(Component.translatable("gui.friends.button.loading.invite_request_pending"));
    private final SpriteIconButton removeButton;
    private final StringWidget statusWidget;
    private @Nullable PresenceStatusDto presence;
    private @Nullable SpriteIconButton leftAction;
    private @Nullable SpriteIconButton rightAction;

    public FriendEntry(
        final Minecraft minecraft,
        final FriendsOverlayScreen screen,
        final PlayerSocialManager.PlayerData playerData,
        final @Nullable PresenceStatusDto presence,
        final boolean initiallyLoading,
        final Runnable onAction
    ) {
        super(minecraft, screen, 0, 0, screen.getOverlayWidth() - 10, 28, playerData, true);
        this.presence = presence;
        this.statusWidget = new StringWidget(
            Component.translatable("gui.friends.presence.status." + (presence == null ? "offline" : presence.status().toString().toLowerCase(Locale.ROOT)))
                .withColor(presence == null ? -6250336 : -16711936),
            minecraft.font
        );
        this.addChild(this.statusWidget);
        Button.CreateNarration narration = getSpriteIconNarration(Component.translatable("gui.friends.narration.button.unfriend", playerData.name()));
        this.removeButton = SpriteIconButton.builder(UNFRIEND, var2 -> this.confirmRemoveFriend(onAction), true)
            .size(20, 20)
            .sprite(REMOVE_SPRITE, 13, 11)
            .tooltip(UNFRIEND)
            .narration(narration)
            .build();
        if (initiallyLoading) {
            this.removeButton.setLoading(true);
        }

        this.addChild(this.removeButton);
        if (presence != null) {
            this.handlePresence(minecraft, playerData, presence);
        }
    }

    void applyPresence(final @Nullable PresenceStatusDto newPresence) {
        this.presence = newPresence;
        this.statusWidget.setMessage(Component.translatable("gui.friends.presence.status." + (newPresence == null ? "offline" : newPresence.status().toString().toLowerCase(Locale.ROOT)))
            .withColor(newPresence == null ? -6250336 : -16711936));
        // Remove existing action buttons
        if (this.leftAction != null) {
            this.removeChild(this.leftAction);
            this.leftAction = null;
        }
        if (this.rightAction != null) {
            this.removeChild(this.rightAction);
            this.rightAction = null;
        }
        // Rebuild actions based on new presence
        if (newPresence != null) {
            PlayerSocialManager.PlayerData playerData = new PlayerSocialManager.PlayerData(this.playerId, this.playerName);
            this.handlePresence(this.minecraft, playerData, newPresence);
        }
    }

    private void handlePresence(final Minecraft minecraft, final PlayerSocialManager.PlayerData playerData, final PresenceStatusDto presence) {
        UUID peerPmid = presence.pmid();
        if (minecraft.p2pManager.hasIncomingJoinRequest(peerPmid)) {
            this.leftAction = SpriteIconButton.builder(ACCEPT_JOIN_REQUEST, var2 -> this.acceptJoinRequest(peerPmid), true)
                .size(20, 20)
                .sprite(ACCEPT_SPRITE, 18, 18)
                .withTootip()
                .build();
            this.addChild(this.leftAction);
            this.rightAction = SpriteIconButton.builder(REJECT_JOIN_REQUEST, var2 -> this.rejectJoinRequest(peerPmid), true)
                .size(20, 20)
                .sprite(REJECT_SPRITE, 18, 18)
                .withTootip()
                .build();
            this.addChild(this.rightAction);
        } else {
            FriendJoinHandler.OutgoingJoinState outgoingJoinState = minecraft.p2pManager.outgoingJoinState(peerPmid);
            if (outgoingJoinState == FriendJoinHandler.OutgoingJoinState.AWAITING_HOST_ACCEPT) {
                SpriteIconButton pendingJoinButton = SpriteIconButton.builder(JOIN_REQUEST, var0 -> {}, true)
                    .size(20, 20)
                    .sprite(JOIN_REQUEST_SPRITE, 7, 11)
                    .tooltip(JOIN_REQUEST)
                    .build();
                pendingJoinButton.setLoading(true, PENDING_JOIN_REQUEST_TOOLTIP);
                this.rightAction = pendingJoinButton;
                this.addChild(this.rightAction);
            } else {
                boolean hasJoinInfo = presence.joinInfo() != null;
                if (hasJoinInfo
                    && outgoingJoinState == FriendJoinHandler.OutgoingJoinState.NONE
                    && presence.joinInfo().invited()
                    && !minecraft.getPlayerSocialManager().getPresenceHandler().hasDismissedInvite(presence)) {
                    if (!minecraft.p2pManager.hasOutgoingJoinRequest()) {
                        this.leftAction = SpriteIconButton.builder(ACCEPT_INVITE, var2 -> this.acceptInvite(peerPmid), true)
                            .size(20, 20)
                            .sprite(ACCEPT_SPRITE, 18, 18)
                            .withTootip()
                            .build();
                        this.addChild(this.leftAction);
                    }

                    this.rightAction = SpriteIconButton.builder(REJECT_INVITE, var2 -> this.declineInvite(peerPmid), true)
                        .size(20, 20)
                        .sprite(REJECT_SPRITE, 18, 18)
                        .withTootip()
                        .build();
                    this.addChild(this.rightAction);
                } else {
                    ClientPacketListener clientConnection = minecraft.getConnection();
                    boolean friendInCurrentWorld = clientConnection != null && clientConnection.getPlayerInfo(playerData.id()) != null;
                    boolean hasOutgoingJoinRequest = minecraft.p2pManager.hasOutgoingJoinRequest();
                    boolean canRequestJoin = hasJoinInfo
                        && outgoingJoinState == FriendJoinHandler.OutgoingJoinState.NONE
                        && !friendInCurrentWorld
                        && !hasOutgoingJoinRequest
                        && presence.status() == PresenceStatus.PLAYING_HOSTED_SERVER;
                    IntegratedServer singleplayerServer = minecraft.getSingleplayerServer();
                    boolean canInvite = presence.status() != PresenceStatus.OFFLINE
                        && singleplayerServer != null
                        && singleplayerServer.getMultiplayerScope() == MinecraftServer.MultiplayerScope.ONLINE
                        && !hasOutgoingJoinRequest
                        && singleplayerServer.getPlayerList().getPlayersByUUID().entrySet().stream().noneMatch(entry -> entry.getKey().equals(playerData.id()));
                    if (canInvite && canRequestJoin) {
                        this.leftAction = SpriteIconButton.builder(INVITE, var3 -> this.invitePlayer(minecraft, playerData.id()), true)
                            .size(20, 20)
                            .sprite(INVITE_SPRITE, 7, 11)
                            .tooltip(INVITE)
                            .switchToLoadingAfterPress()
                            .build();
                        this.leftAction
                            .setLoading(
                                minecraft.getPlayerSocialManager().getPresenceHandler().getInvitedPlayersBatch().contains(playerData.id()),
                                PENDING_INVITE_REQUEST_TOOLTIP
                            );
                        this.addChild(this.leftAction);
                        this.rightAction = SpriteIconButton.builder(JOIN_REQUEST, var3 -> this.requestToJoinPlayer(peerPmid, presence.profileId()), true)
                            .size(20, 20)
                            .sprite(JOIN_REQUEST_SPRITE, 7, 11)
                            .tooltip(JOIN_REQUEST)
                            .switchToLoadingAfterPress()
                            .build();
                        this.addChild(this.rightAction);
                    } else if (canRequestJoin) {
                        this.rightAction = SpriteIconButton.builder(JOIN_REQUEST, var3 -> this.requestToJoinPlayer(peerPmid, presence.profileId()), true)
                            .size(20, 20)
                            .sprite(JOIN_REQUEST_SPRITE, 7, 11)
                            .tooltip(JOIN_REQUEST)
                            .switchToLoadingAfterPress()
                            .build();
                        this.addChild(this.rightAction);
                    } else if (canInvite) {
                        this.rightAction = SpriteIconButton.builder(INVITE, var3 -> this.invitePlayer(minecraft, playerData.id()), true)
                            .size(20, 20)
                            .sprite(INVITE_SPRITE, 7, 11)
                            .tooltip(INVITE)
                            .switchToLoadingAfterPress()
                            .build();
                        this.rightAction
                            .setLoading(
                                minecraft.getPlayerSocialManager().getPresenceHandler().getInvitedPlayersBatch().contains(playerData.id()),
                                PENDING_INVITE_REQUEST_TOOLTIP
                            );
                        this.addChild(this.rightAction);
                    }
                }
            }
        }
    }

    private void invitePlayer(final Minecraft minecraft, final UUID id) {
        minecraft.getPlayerSocialManager().getPresenceHandler().invitePlayer(id);
        this.showTooltipFor(FriendToast::showFriendInvited, id);
    }

    private void requestToJoinPlayer(final UUID pmid, final UUID profileId) {
        this.showTooltipFor(FriendToast::showRequestToJoinFriend, profileId);
        this.minecraft.p2pManager.joinPlayer(pmid.toString()).whenCompleteAsync((var1, var2) -> this.screen.refreshLists(), this.minecraft);
    }

    private void showTooltipFor(final TriConsumer<Minecraft, String, PlayerSkin> toastData, final UUID profileId) {
        Optional<PlayerSocialManager.PlayerData> friendData = this.minecraft
            .getPlayerSocialManager()
            .getFriends()
            .stream()
            .filter(playerData -> playerData.id().equals(profileId))
            .findAny();
        friendData.ifPresent(friend -> {
            PlayerSkin friendSkin = this.minecraft.playerSkinRenderCache().getOrDefault(ResolvableProfile.createUnresolved(friend.id())).playerSkin();
            toastData.accept(this.minecraft, friend.name(), friendSkin);
        });
    }

    private void acceptInvite(final UUID pmid) {
        this.minecraft.getPlayerSocialManager().getPresenceHandler().dismissInviteForPmid(pmid);
        this.minecraft.p2pManager.joinPlayer(pmid.toString()).whenCompleteAsync((var1, var2) -> this.screen.refreshLists(), this.minecraft);
    }

    private void declineInvite(final UUID pmid) {
        this.minecraft.p2pManager.declineInvite(pmid).thenRunAsync(() -> {
            this.minecraft.getPlayerSocialManager().getPresenceHandler().dismissInviteForPmid(pmid);
            this.screen.refreshLists();
        }, this.minecraft).exceptionally(var0 -> null);
    }

    private void acceptJoinRequest(final UUID pmid) {
        this.minecraft.p2pManager.acceptIncomingJoinRequest(pmid);
        this.screen.refreshLists();
    }

    private void rejectJoinRequest(final UUID pmid) {
        this.minecraft.p2pManager.rejectIncomingJoinRequest(pmid);
        this.screen.refreshLists();
    }

    public int presenceStatusSortOrder() {
        PresenceStatus status = this.presence == null ? PresenceStatus.OFFLINE : this.presence.status();

        return switch (status) {
            case PLAYING_HOSTED_SERVER -> 0;
            case PLAYING_SERVER -> 1;
            case PLAYING_REALMS -> 2;
            case PLAYING_OFFLINE -> 3;
            case ONLINE -> 4;
            case OFFLINE -> 5;
            default -> throw new MatchException(null, null);
        };
    }

    @Override
    void disable() {
        this.removeButton.active = false;
        if (this.leftAction != null) {
            this.leftAction.active = false;
        }

        if (this.rightAction != null) {
            this.rightAction.active = false;
        }
    }

    @Override
    protected Component getEntryNarration() {
        return Component.translatable("gui.friends.narration.entry.friend", this.playerName);
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        int buttonStep = 22;
        int verticalCenter = this.getY() + (this.getHeight() - 20) / 2;
        int removeX = this.getX() + this.getWidth() - 20 + 2;
        this.removeButton.setPosition(removeX, verticalCenter);
        this.removeButton.extractRenderState(graphics, mouseX, mouseY, a);
        int statusWidgetX = this.playerFaceWidget.getRight() + 4;
        int statusWidth = removeX - statusWidgetX - 2;
        if (this.rightAction != null) {
            this.rightAction.setPosition(removeX - 22, verticalCenter);
            this.rightAction.extractRenderState(graphics, mouseX, mouseY, a);
            statusWidth -= 22;
        }

        if (this.leftAction != null) {
            this.leftAction.setPosition(removeX - 44, verticalCenter);
            this.leftAction.extractRenderState(graphics, mouseX, mouseY, a);
            statusWidth -= 22;
        }

        this.statusWidget.setMaxWidth(statusWidth, StringWidget.TextOverflow.SCROLLING);
        this.statusWidget.setPosition(statusWidgetX, this.nameWidget.getBottom() + 2);
        this.statusWidget.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void confirmRemoveFriend(final Runnable action) {
        this.minecraft
            .gui
            .setScreen(new PopupScreen.Builder(this.screen, CONFIRM_TITLE).addMessage(CONFIRM_UNFRIEND).addButton(CommonComponents.GUI_REMOVE, var2 -> {
                this.removeButton.setLoading(true);
                this.screen.startFriendAction();
                action.run();
                this.minecraft.gui.setScreen(this.screen);
            }).addButton(CommonComponents.GUI_CANCEL, var1 -> this.minecraft.gui.setScreen(this.screen)).build());
    }
}