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
    public static final LastSeenMessages EMPTY = new LastSeenMessages(List.of());
    public static final int LAST_SEEN_MESSAGES_MAX_LENGTH = 20;

    public void updateSignature(final SignatureUpdater.Output output) throws SignatureException {
        output.update(Ints.toByteArray(this.entries.size()));

        for (MessageSignature entry : this.entries) {
            output.update(entry.bytes());
        }
    }

    public LastSeenMessages.Packed pack(final MessageSignatureCache cache) {
        return new LastSeenMessages.Packed(this.entries.stream().map(entry -> entry.pack(cache)).toList());
    }

    public byte computeChecksum() {
        int checksum = 1;

        for (MessageSignature entry : this.entries) {
            checksum = 31 * checksum + entry.checksum();
        }

        byte checksumByte = (byte)checksum;
        return checksumByte == 0 ? 1 : checksumByte;
    }

    public record Packed(List<MessageSignature.Packed> entries) {
        public static final LastSeenMessages.Packed EMPTY = new LastSeenMessages.Packed(List.of());

        public Packed(final FriendlyByteBuf input) {
            this(input.<MessageSignature.Packed, List<MessageSignature.Packed>>readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), MessageSignature.Packed::read));
        }

        public void write(final FriendlyByteBuf output) {
            output.writeCollection(this.entries, MessageSignature.Packed::write);
        }

        public Optional<LastSeenMessages> unpack(final MessageSignatureCache cache) {
            List<MessageSignature> unpacked = new ArrayList<>(this.entries.size());

            for (MessageSignature.Packed packed : this.entries) {
                Optional<MessageSignature> entry = packed.unpack(cache);
                if (entry.isEmpty()) {
                    return Optional.empty();
                }

                unpacked.add(entry.get());
            }

            return Optional.of(new LastSeenMessages(unpacked));
        }
    }

    public record Update(int offset, BitSet acknowledged, byte checksum) {
        public static final byte IGNORE_CHECKSUM = 0;

        public Update(final FriendlyByteBuf input) {
            this(input.readVarInt(), input.readFixedBitSet(20), input.readByte());
        }

        public void write(final FriendlyByteBuf output) {
            output.writeVarInt(this.offset);
            output.writeFixedBitSet(this.acknowledged, 20);
            output.writeByte(this.checksum);
        }

        public boolean verifyChecksum(final LastSeenMessages lastSeen) {
            return this.checksum == 0 || this.checksum == lastSeen.computeChecksum();
        }
    }
}
