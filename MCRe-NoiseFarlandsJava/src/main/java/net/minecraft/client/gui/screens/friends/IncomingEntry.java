package net.minecraft.client.gui.screens.friends;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class IncomingEntry extends AbstractFriendsEntryContainerWidget {
    private static final WidgetSprites ACCEPT_SPRITE = new WidgetSprites(
        Identifier.withDefaultNamespace("friends/accept"), Identifier.withDefaultNamespace("friends/accept_highlighted")
    );
    private static final WidgetSprites REJECT_SPRITE = new WidgetSprites(
        Identifier.withDefaultNamespace("friends/reject"), Identifier.withDefaultNamespace("friends/reject_highlighted")
    );
    private static final Component ACCEPT_INVITE = Component.translatable("gui.friends.accept");
    private static final Component REJECT_INVITE = Component.translatable("gui.friends.decline");
    private final SpriteIconButton acceptButton;
    private final SpriteIconButton rejectButton;

    public IncomingEntry(
        final Minecraft minecraft,
        final FriendsOverlayScreen screen,
        final PlayerSocialManager.PlayerData playerData,
        final Runnable acceptAction,
        final Runnable declineAction
    ) {
        super(minecraft, screen, 0, 0, screen.getOverlayWidth() - 16, 28, playerData);
        Button.CreateNarration acceptNarration = getSpriteIconNarration(Component.translatable("gui.friends.narration.button.accept", playerData.name()));
        Button.CreateNarration rejectNarration = getSpriteIconNarration(Component.translatable("gui.friends.narration.button.decline", playerData.name()));
        this.acceptButton = SpriteIconButton.builder(ACCEPT_INVITE, var2 -> {
            screen.startFriendAction();
            acceptAction.run();
        }, true).size(20, 20).sprite(ACCEPT_SPRITE, 18, 18).tooltip(ACCEPT_INVITE).narration(acceptNarration).switchToLoadingAfterPress().build();
        this.addChild(this.acceptButton);
        this.rejectButton = SpriteIconButton.builder(REJECT_INVITE, var2 -> {
            screen.startFriendAction();
            declineAction.run();
        }, true).size(20, 20).sprite(REJECT_SPRITE, 18, 18).tooltip(REJECT_INVITE).narration(rejectNarration).switchToLoadingAfterPress().build();
        this.addChild(this.rejectButton);
    }

    @Override
    void disable() {
        this.acceptButton.active = false;
        this.rejectButton.active = false;
    }

    @Override
    protected Component getEntryNarration() {
        return Component.translatable("gui.friends.narration.entry.incoming", this.playerName);
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        this.rejectButton
            .setPosition(this.getX() + this.getWidth() - this.rejectButton.getWidth(), this.getY() + (this.getHeight() - this.rejectButton.getHeight()) / 2);
        this.rejectButton.extractRenderState(graphics, mouseX, mouseY, a);
        this.acceptButton
            .setPosition(this.rejectButton.getX() - this.acceptButton.getWidth() - 4, this.getY() + (this.getHeight() - this.acceptButton.getHeight()) / 2);
        this.acceptButton.extractRenderState(graphics, mouseX, mouseY, a);
    }
}