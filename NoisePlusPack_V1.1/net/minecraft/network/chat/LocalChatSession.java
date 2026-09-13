package net.minecraft.network.chat;

import java.util.UUID;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfileKeyPair;

public record LocalChatSession(UUID sessionId, ProfileKeyPair keyPair) {
   public static LocalChatSession create(ProfileKeyPair p_250798_) {
      return new LocalChatSession(UUID.randomUUID(), p_250798_);
   }

   public SignedMessageChain.Encoder createMessageEncoder(UUID p_251085_) {
      return new SignedMessageChain(p_251085_, this.sessionId).encoder(Signer.from(this.keyPair.privateKey(), "SHA256withRSA"));
   }

   public RemoteChatSession asRemote() {
      return new RemoteChatSession(this.sessionId, this.keyPair.publicKey());
   }
}
