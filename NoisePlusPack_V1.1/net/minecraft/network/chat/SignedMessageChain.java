package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SignedMessageChain {
   static final Logger LOGGER = LogUtils.getLogger();
   @Nullable SignedMessageLink nextLink;
   Instant lastTimeStamp = Instant.EPOCH;

   public SignedMessageChain(UUID p_250050_, UUID p_249127_) {
      this.nextLink = SignedMessageLink.root(p_250050_, p_249127_);
   }

   public SignedMessageChain.Encoder encoder(Signer p_248636_) {
      return p_326076_ -> {
         SignedMessageLink signedmessagelink = this.nextLink;
         if (signedmessagelink == null) {
            return null;
         }

         this.nextLink = signedmessagelink.advance();
         return new MessageSignature(p_248636_.sign(p_248065_ -> PlayerChatMessage.updateSignature(p_248065_, signedmessagelink, p_326076_)));
      };
   }

   public SignedMessageChain.Decoder decoder(final ProfilePublicKey p_249122_) {
      final SignatureValidator signaturevalidator = p_249122_.createSignatureValidator();
      return new SignedMessageChain.Decoder() {
         @Override
         public PlayerChatMessage unpack(@Nullable MessageSignature p_328199_, SignedMessageBody p_328915_) throws SignedMessageChain.DecodeException {
            if (p_328199_ == null) {
               throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.MISSING_PROFILE_KEY);
            }

            if (p_249122_.data().hasExpired()) {
               throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.EXPIRED_PROFILE_KEY);
            }

            SignedMessageLink signedmessagelink = SignedMessageChain.this.nextLink;
            if (signedmessagelink == null) {
               throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.CHAIN_BROKEN);
            }

            if (p_328915_.timeStamp().isBefore(SignedMessageChain.this.lastTimeStamp)) {
               this.setChainBroken();
               throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.OUT_OF_ORDER_CHAT);
            }

            SignedMessageChain.this.lastTimeStamp = p_328915_.timeStamp();
            PlayerChatMessage playerchatmessage = new PlayerChatMessage(signedmessagelink, p_328199_, p_328915_, null, FilterMask.PASS_THROUGH);
            if (!playerchatmessage.verify(signaturevalidator)) {
               this.setChainBroken();
               throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.INVALID_SIGNATURE);
            }

            if (playerchatmessage.hasExpiredServer(Instant.now())) {
               SignedMessageChain.LOGGER.warn("Received expired chat: '{}'. Is the client/server system time unsynchronized?", p_328915_.content());
            }

            SignedMessageChain.this.nextLink = signedmessagelink.advance();
            return playerchatmessage;
         }

         @Override
         public void setChainBroken() {
            SignedMessageChain.this.nextLink = null;
         }
      };
   }

   public static class DecodeException extends ThrowingComponent {
      static final Component MISSING_PROFILE_KEY = Component.translatable("chat.disabled.missingProfileKey");
      static final Component CHAIN_BROKEN = Component.translatable("chat.disabled.chain_broken");
      static final Component EXPIRED_PROFILE_KEY = Component.translatable("chat.disabled.expiredProfileKey");
      static final Component INVALID_SIGNATURE = Component.translatable("chat.disabled.invalid_signature");
      static final Component OUT_OF_ORDER_CHAT = Component.translatable("chat.disabled.out_of_order_chat");

      public DecodeException(Component p_249149_) {
         super(p_249149_);
      }
   }

   @FunctionalInterface
   public interface Decoder {
      static SignedMessageChain.Decoder unsigned(UUID p_251747_, BooleanSupplier p_312636_) {
         return (p_326079_, p_326080_) -> {
            if (p_312636_.getAsBoolean()) {
               throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.MISSING_PROFILE_KEY);
            } else {
               return PlayerChatMessage.unsigned(p_251747_, p_326080_.content());
            }
         };
      }

      PlayerChatMessage unpack(@Nullable MessageSignature var1, SignedMessageBody var2) throws SignedMessageChain.DecodeException;

      default void setChainBroken() {
      }
   }

   @FunctionalInterface
   public interface Encoder {
      SignedMessageChain.Encoder UNSIGNED = p_250548_ -> null;

      @Nullable MessageSignature pack(SignedMessageBody var1);
   }
}
