package net.minecraft.network.chat;

import net.minecraft.server.level.ServerPlayer;

public interface OutgoingChatMessage {
   Component content();

   void sendToPlayer(ServerPlayer player, boolean filtered, ChatType.Bound chatType);

   static OutgoingChatMessage create(final PlayerChatMessage message) {
      return message.isSystem() ? new OutgoingChatMessage.Disguised(message.decoratedContent()) : new OutgoingChatMessage.Player(message);
   }

   record Disguised(Component content) implements OutgoingChatMessage {
      @Override
      public void sendToPlayer(final ServerPlayer player, final boolean filtered, final ChatType.Bound chatType) {
         player.connection.sendDisguisedChatMessage(this.content, chatType);
      }
   }

   record Player(PlayerChatMessage message) implements OutgoingChatMessage {
      @Override
      public Component content() {
         return this.message.decoratedContent();
      }

      @Override
      public void sendToPlayer(final ServerPlayer player, final boolean filtered, final ChatType.Bound chatType) {
         PlayerChatMessage filteredMessage = this.message.filter(filtered);
         if (!filteredMessage.isFullyFiltered()) {
            player.connection.sendPlayerChatMessage(filteredMessage, chatType);
         }
      }
   }
}
