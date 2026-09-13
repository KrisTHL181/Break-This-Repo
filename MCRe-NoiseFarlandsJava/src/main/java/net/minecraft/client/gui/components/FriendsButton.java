package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FriendsButton extends SpriteIconButton.CenteredIcon {
    private static final Component TOOLTIP = Component.translatable("gui.friends.open.tooltip");
    private static final Component MESSAGE = Component.translatable("gui.friends.open");
    private static final Button.CreateNarration NARRATION = var0 -> Component.translatable("gui.friends.open.narration");
    private static final Identifier[] NOTIFICATION_ICONS = new Identifier[]{
        Identifier.withDefaultNamespace("notification/1"),
        Identifier.withDefaultNamespace("notification/2"),
        Identifier.withDefaultNamespace("notification/3"),
        Identifier.withDefaultNamespace("notification/4"),
        Identifier.withDefaultNamespace("notification/5"),
        Identifier.withDefaultNamespace("notification/more")
    };
    private static final int SPRITE_SIZE = 15;
    private int incomingRequestCount;

    public FriendsButton(final int width, final Button.OnPress onPress, final boolean friendsAvailable) {
        super(
            width,
            20,
            MESSAGE,
            15,
            15,
            0,
            0,
            new WidgetSprites(Identifier.withDefaultNamespace("friends/friends")),
            onPress,
            friendsAvailable ? TOOLTIP : null,
            NARRATION,
            false
        );
        this.active = friendsAvailable;
        this.refreshIncomingRequestCount();
    }

    public void refreshIncomingRequestCount() {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerSocialManager playerSocialManager = minecraft.getPlayerSocialManager();
        if (!playerSocialManager.isFriendListEnabled()) {
            this.incomingRequestCount = 0;
        } else {
            this.incomingRequestCount = playerSocialManager.getIncomingRequests().size();
        }
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        if (this.isActive() && this.incomingRequestCount > 0) {
            int iconIndex = Math.min(this.incomingRequestCount, 6) - 1;
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, NOTIFICATION_ICONS[iconIndex], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8, this.alpha
            );
        }
    }
}