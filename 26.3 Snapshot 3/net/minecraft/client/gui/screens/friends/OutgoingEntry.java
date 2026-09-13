package net.minecraft.client.gui.screens.friends;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

class OutgoingEntry extends AbstractFriendsEntryContainerWidget {
   private static final WidgetSprites REVOKE_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/cancel"));
   private static final Component REVOKE_INVITE = Component.translatable("gui.friends.cancel_request");
   private final SpriteIconButton revokeButton;

   public OutgoingEntry(
      final Minecraft minecraft, final FriendsOverlayScreen screen, final PlayerSocialManager.PlayerData playerData, final Runnable revokeAction
   ) {
      super(minecraft, screen, 0, 0, screen.getOverlayWidth() - 16, 28, playerData);
      Button.CreateNarration narration = getSpriteIconNarration(Component.translatable("gui.friends.narration.button.cancel_request", playerData.name()));
      this.revokeButton = SpriteIconButton.builder(REVOKE_INVITE, var2 -> {
         screen.startFriendAction();
         revokeAction.run();
      }, true).size(20, 20).sprite(REVOKE_SPRITE, 12, 12).tooltip(REVOKE_INVITE).narration(narration).switchToLoadingAfterPress().build();
      this.addChild(this.revokeButton);
   }

   @Override
   void disable() {
      this.revokeButton.active = false;
   }

   @Override
   protected Component getEntryNarration() {
      return Component.translatable("gui.friends.narration.entry.outgoing", this.playerName);
   }

   @Override
   protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
      this.revokeButton
         .setPosition(this.getX() + this.getWidth() - this.revokeButton.getWidth(), this.getY() + (this.getHeight() - this.revokeButton.getHeight()) / 2);
      this.revokeButton.extractRenderState(graphics, mouseX, mouseY, a);
   }
}
