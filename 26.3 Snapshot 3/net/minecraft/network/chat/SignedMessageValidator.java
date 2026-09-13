package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.util.function.BooleanSupplier;
import net.minecraft.util.SignatureValidator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@FunctionalInterface
public interface SignedMessageValidator {
   Logger LOGGER = LogUtils.getLogger();
   SignedMessageValidator ACCEPT_UNSIGNED = PlayerChatMessage::removeSignature;
   SignedMessageValidator REJECT_ALL = message -> {
      LOGGER.error("Received chat message from {}, but they have no chat session initialized and secure chat is enforced", message.sender());
      return null;
   };

   @Nullable PlayerChatMessage updateAndValidate(PlayerChatMessage message);

   class KeyBased implements SignedMessageValidator {
      private final SignatureValidator validator;
      private final BooleanSupplier expired;
      private @Nullable PlayerChatMessage lastMessage;
      private boolean isChainValid = true;

      public KeyBased(final SignatureValidator validator, final BooleanSupplier expired) {
         this.validator = validator;
         this.expired = expired;
      }

      private boolean validateChain(final PlayerChatMessage message) {
         if (message.equals(this.lastMessage)) {
            return true;
         } else if (this.lastMessage != null && !message.link().isDescendantOf(this.lastMessage.link())) {
            LOGGER.error(
               "Received out-of-order chat message from {}: expected index > {} for session {}, but was {} for session {}",
               new Object[]{
                  message.sender(), this.lastMessage.link().index(), this.lastMessage.link().sessionId(), message.link().index(), message.link().sessionId()
               }
            );
            return false;
         } else {
            return true;
         }
      }

      private boolean validate(final PlayerChatMessage message) {
         if (this.expired.getAsBoolean()) {
            LOGGER.error("Received message with expired profile public key from {} with session {}", message.sender(), message.link().sessionId());
            return false;
         } else if (!message.verify(this.validator)) {
            LOGGER.error(
               "Received message with invalid signature (is the session wrong, or signature cache out of sync?): {}", PlayerChatMessage.describeSigned(message)
            );
            return false;
         } else {
            return this.validateChain(message);
         }
      }

      @Override
      public @Nullable PlayerChatMessage updateAndValidate(final PlayerChatMessage message) {
         this.isChainValid = this.isChainValid && this.validate(message);
         if (!this.isChainValid) {
            return null;
         }

         this.lastMessage = message;
         return message;
      }
   }
}
