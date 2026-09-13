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

   public ClientboundStopSoundPacket(@Nullable Identifier p_459295_, @Nullable SoundSource p_133469_) {
      this.name = p_459295_;
      this.source = p_133469_;
   }

   private ClientboundStopSoundPacket(FriendlyByteBuf p_179426_) {
      int i = p_179426_.readByte();
      if ((i & 1) > 0) {
         this.source = p_179426_.readEnum(SoundSource.class);
      } else {
         this.source = null;
      }

      if ((i & 2) > 0) {
         this.name = p_179426_.readIdentifier();
      } else {
         this.name = null;
      }
   }

   private void write(FriendlyByteBuf p_133478_) {
      if (this.source != null) {
         if (this.name != null) {
            p_133478_.writeByte(3);
            p_133478_.writeEnum(this.source);
            p_133478_.writeIdentifier(this.name);
         } else {
            p_133478_.writeByte(1);
            p_133478_.writeEnum(this.source);
         }
      } else if (this.name != null) {
         p_133478_.writeByte(2);
         p_133478_.writeIdentifier(this.name);
      } else {
         p_133478_.writeByte(0);
      }
   }

   @Override
   public PacketType<ClientboundStopSoundPacket> type() {
      return GamePacketTypes.CLIENTBOUND_STOP_SOUND;
   }

   public void handle(ClientGamePacketListener p_133475_) {
      p_133475_.handleStopSoundEvent(this);
   }

   public @Nullable Identifier getName() {
      return this.name;
   }

   public @Nullable SoundSource getSource() {
      return this.source;
   }
}
