package net.minecraft.client.gui.components.toasts;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FriendToast implements Toast {
    private static final WidgetSprites BACKGROUND_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/toast_background"));
    private static final int FACE_SIZE = 20;
    private static final int TEXT_LEFT_WITH_FACE = 30;
    private static final int TEXT_LEFT_NO_FACE = 7;
    private static final int PADDING_TOP = 7;
    private static final int PADDING_BOTTOM = 3;
    private static final int LINE_SPACING = 11;
    private static final long DEFAULT_DISPLAY_TIME_MS = 5000L;
    private final @Nullable PlayerSkin skin;
    private final List<FormattedCharSequence> messageLines;
    private final long displayTimeMs;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;

    public FriendToast(final Font font, final @Nullable PlayerSkin skin, final Component message) {
        this(font, skin, message, 5000L);
    }

    public FriendToast(final Font font, final @Nullable PlayerSkin skin, final Component message, final long displayTimeMs) {
        this.skin = skin;
        int textLeft = skin != null ? 30 : 7;
        this.messageLines = font.split(message, 160 - textLeft - 4);
        this.displayTimeMs = displayTimeMs;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(final ToastManager manager, final long fullyVisibleForMs) {
        if (fullyVisibleForMs >= this.displayTimeMs * manager.getNotificationDisplayTimeMultiplier()) {
            this.visibility = Toast.Visibility.HIDE;
        }
    }

    @Override
    public int height() {
        return 7 + this.contentHeight() + 3;
    }

    private int contentHeight() {
        return Math.max(this.messageLines.size(), 2) * 11;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final Font font, final long fullyVisibleForMs) {
        int height = this.height();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE.get(true, false), 0, 0, this.width(), height);
        int textLeft;
        if (this.skin != null) {
            PlayerFaceExtractor.extractRenderState(graphics, this.skin, 6, 6, 20);
            textLeft = 30;
        } else {
            textLeft = 7;
        }

        int totalTextHeight = this.messageLines.size() * 11;
        int textTop = 7 + (this.contentHeight() - totalTextHeight) / 2;

        for (int i = 0; i < this.messageLines.size(); i++) {
            graphics.text(font, this.messageLines.get(i), textLeft, textTop + i * 11, -1, false);
        }
    }

    public void hide() {
        this.visibility = Toast.Visibility.HIDE;
    }

    public static void add(final ToastManager toastManager, final Font font, final @Nullable PlayerSkin skin, final Component message) {
        toastManager.addToast(new FriendToast(font, skin, message));
    }

    private static void add(final Minecraft minecraft, final @Nullable PlayerSkin skin, final Component message) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, message);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface SkinToastEmitter {
        void emit(Minecraft minecraft, String playerName, UUID playerId);
    }

    private static void showToastFor(final Minecraft minecraft, final UUID playerId, final Component message, final SkinToastEmitter emitter) {
        net.minecraft.client.gui.screens.social.PlayerSocialManager.PlayerData friendData = minecraft.getPlayerSocialManager().getFriends().stream()
            .filter(playerData -> playerData.id().equals(playerId))
            .findAny()
            .orElse(null);
        if (friendData != null) {
            emitter.emit(minecraft, friendData.name(), friendData.id());
        }
    }

    public static void showFriendRequestSent(final Minecraft minecraft, final String nickname) {
        add(minecraft, null, Component.translatable("gui.friends.toast.request_sent.message", nickname));
    }

    public static void showFriendRequestReceived(final Minecraft minecraft, final String nickname, final UUID playerId) {
        showToastFor(minecraft, playerId, Component.translatable("gui.friends.toast.request_received.message", nickname), FriendToast::addWithSkin);
    }

    public static void showFriendRequestAccepted(final Minecraft minecraft, final String nickname, final UUID playerId) {
        showToastFor(minecraft, playerId, Component.translatable("gui.friends.toast.request_accepted.message", nickname), FriendToast::addWithSkin);
    }

    public static void showFriendAdded(final Minecraft minecraft, final String nickname, final UUID playerId) {
        showToastFor(minecraft, playerId, Component.translatable("gui.friends.toast.friend_added.message", nickname), FriendToast::addWithSkin);
    }

    private static void addWithSkin(final Minecraft minecraft, final String playerName, final UUID playerId) {
        ResolvableProfile skinProfile = ResolvableProfile.createUnresolved(playerId);
        PlayerSkin skin = minecraft.playerSkinRenderCache().getOrDefault(skinProfile).playerSkin();
        add(minecraft, skin, Component.translatable("gui.friends.toast.friend_added.message", playerName));
    }

    public static void showFriendJoinRequest(final Minecraft minecraft, final String profileName, final PlayerSkin skin) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, Component.translatable("gui.friends.toast.join_request.message", profileName, minecraft.options.keyFriends.getTranslatedKeyMessage()));
    }

    public static void showFriendInvited(final Minecraft minecraft, final String profileName, final PlayerSkin skin) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, Component.translatable("gui.friends.toast.friend_invited.message", profileName));
    }

    public static void showInviteFromFriend(final Minecraft minecraft, final String profileName, final PlayerSkin skin) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, Component.translatable("gui.friends.toast.invite_from_friend.message", profileName, minecraft.options.keyFriends.getTranslatedKeyMessage()));
    }

    public static void showRequestToJoinFriend(final Minecraft minecraft, final String profileName, final PlayerSkin skin) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, Component.translatable("gui.friends.toast.request_to_join_friend.message", profileName));
    }

    public static void showHostInviteExpired(final Minecraft minecraft, final String profileName, final @Nullable PlayerSkin skin) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, Component.translatable("gui.friends.toast.host_invite_expired.message", profileName));
    }

    public static void showJoinInviteExpired(final Minecraft minecraft, final String profileName, final @Nullable PlayerSkin skin) {
        add(minecraft.gui.toastManager(), minecraft.font, skin, Component.translatable("gui.friends.toast.join_invite_expired.message", profileName));
    }
}