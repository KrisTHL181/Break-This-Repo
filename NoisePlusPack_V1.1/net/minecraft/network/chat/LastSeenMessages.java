package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.mojang.serialization.Codec;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureUpdater;

public record LastSeenMessages(List<MessageSignature> entries) {
   public static final Codec<LastSeenMessages> CODEC = MessageSignature.CODEC.listOf().xmap(LastSeenMessages::new, LastSeenMessages::entries);
   public static LastSeenMessages EMPTY = new LastSeenMessages(List.of());
   public static final int LAST_SEEN_MESSAGES_MAX_LENGTH = 20;

   public void updateSignature(SignatureUpdater.Output p_251665_) throws SignatureException {
      p_251665_.update(Ints.toByteArray(this.entries.size()));

      for (MessageSignature messagesignature : this.entries) {
         p_251665_.update(messagesignature.bytes());
      }
   }

   public LastSeenMessages.Packed pack(MessageSignatureCache p_253961_) {
      return new LastSeenMessages.Packed(this.entries.stream().map(p_253457_ -> p_253457_.pack(p_253961_)).toList());
   }

   public byte computeChecksum() {
      int i = 1;

      for (MessageSignature messagesignature : this.entries) {
         i = 31 * i + messagesignature.checksum();
      }

      byte b0 = (byte)i;
      return b0 == 0 ? 1 : b0;
   }

   public record Packed(List<MessageSignature.Packed> entries) {
      public static final LastSeenMessages.Packed EMPTY = new LastSeenMessages.Packed(List.of());

      public Packed(FriendlyByteBuf p_249757_) {
         this(p_249757_.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), MessageSignature.Packed::read));
      }

      public void write(FriendlyByteBuf p_250725_) {
         p_250725_.writeCollection(this.entries, MessageSignature.Packed::write);
      }

      public Optional<LastSeenMessages> unpack(MessageSignatureCache p_253745_) {
         List<MessageSignature> list = new ArrayList<>(this.entries.size());

         for (MessageSignature.Packed messagesignature$packed : this.entries) {
            Optional<MessageSignature> optional = messagesignature$packed.unpack(p_253745_);
            if (optional.isEmpty()) {
               return Optional.empty();
            }

            list.add(optional.get());
         }

         return Optional.of(new LastSeenMessages(list));
      }
   }

   public record Update(int offset, BitSet acknowledged, byte checksum) {
      public static final byte IGNORE_CHECKSUM = 0;

      public Update(FriendlyByteBuf p_242184_) {
         this(p_242184_.readVarInt(), p_242184_.readFixedBitSet(20), p_242184_.readByte());
      }

      public void write(FriendlyByteBuf p_242221_) {
         p_242221_.writeVarInt(this.offset);
         p_242221_.writeFixedBitSet(this.acknowledged, 20);
         p_242221_.writeByte(this.checksum);
      }

      public boolean verifyChecksum(LastSeenMessages p_395069_) {
         return this.checksum == 0 || this.checksum == p_395069_.computeChecksum();
      }
   }
}
