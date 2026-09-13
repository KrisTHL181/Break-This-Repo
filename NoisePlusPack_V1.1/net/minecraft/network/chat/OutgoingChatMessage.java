package net.minecraft.network.chat;

import net.minecraft.server.level.ServerPlayer;

public interface OutgoingChatMessage {
   Component content();

   void sendToPlayer(ServerPlayer var1, boolean var2, ChatType.Bound var3);

   static OutgoingChatMessage create(PlayerChatMessage p_249173_) {
      return (OutgoingChatMessage)(
         p_249173_.isSystem() ? new OutgoingChatMessage.Disguised(p_249173_.decoratedContent()) : new OutgoingChatMessage.Player(p_249173_)
      );
   }

   record Disguised(Component content) implements OutgoingChatMessage {
      @Override
      public void sendToPlayer(ServerPlayer p_249237_, boolean p_249574_, ChatType.Bound p_250880_) {
         p_249237_.connection.sendDisguisedChatMessage(this.content, p_250880_);
      }
   }

   record Player(PlayerChatMessage message) implements OutgoingChatMessage {
      @Override
      public Component content() {
         return this.message.decoratedContent();
      }

      @Override
      public void sendToPlayer(ServerPlayer p_249642_, boolean p_251123_, ChatType.Bound p_251482_) {
         PlayerChatMessage playerchatmessage = this.message.filter(p_251123_);
         if (!playerchatmessage.isFullyFiltered()) {
            p_249642_.connection.sendPlayerChatMessage(playerchatmessage, p_251482_);
         }
      }
   }
}
