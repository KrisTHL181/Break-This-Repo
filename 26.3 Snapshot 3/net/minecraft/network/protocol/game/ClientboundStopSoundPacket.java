package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public class ClientboundStopSoundPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundStopSoundPacket> STREAM_CODEC = Packet.codec(
      ClientboundStopSoundPacket::write, ClientboundStopSoundPacket::new
   );
   private static final int HAS_SOURCE = 1;
   private static final int HAS_SOUND = 2;
   private final @Nullable Identifier name;
   private final @Nullable SoundSource source;

   public ClientboundStopSoundPacket(final @Nullable Identifier name, final @Nullable SoundSource source) {
      this.name = name;
      this.source = source;
   }

   private ClientboundStopSoundPacket(final FriendlyByteBuf input) {
      int flags = input.readByte();
      if ((flags & 1) > 0) {
         this.source = input.readEnum(SoundSource.class);
      } else {
         this.source = null;
      }

      if ((flags & 2) > 0) {
         this.name = input.readIdentifier();
      } else {
         this.name = null;
      }
   }

   private void write(final FriendlyByteBuf output) {
      if (this.source != null) {
         if (this.name != null) {
            output.writeByte(3);
            output.writeEnum(this.source);
            output.writeIdentifier(this.name);
         } else {
            output.writeByte(1);
            output.writeEnum(this.source);
         }
      } else if (this.name != null) {
         output.writeByte(2);
         output.writeIdentifier(this.name);
      } else {
         output.writeByte(0);
      }
   }

   @Override
   public PacketType<ClientboundStopSoundPacket> type() {
      return GamePacketTypes.CLIENTBOUND_STOP_SOUND;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleStopSoundEvent(this);
   }

   public @Nullable Identifier getName() {
      return this.name;
   }

   public @Nullable SoundSource getSource() {
      return this.source;
   }
}
