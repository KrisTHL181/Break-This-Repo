package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetTitlesAnimationPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetTitlesAnimationPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetTitlesAnimationPacket::write, ClientboundSetTitlesAnimationPacket::new
   );
   private final int fadeIn;
   private final int stay;
   private final int fadeOut;

   public ClientboundSetTitlesAnimationPacket(final int fadeIn, final int stay, final int fadeOut) {
      this.fadeIn = fadeIn;
      this.stay = stay;
      this.fadeOut = fadeOut;
   }

   private ClientboundSetTitlesAnimationPacket(final FriendlyByteBuf input) {
      this.fadeIn = input.readInt();
      this.stay = input.readInt();
      this.fadeOut = input.readInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeInt(this.fadeIn);
      output.writeInt(this.stay);
      output.writeInt(this.fadeOut);
   }

   @Override
   public PacketType<ClientboundSetTitlesAnimationPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_TITLES_ANIMATION;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.setTitlesAnimation(this);
   }

   public int getFadeIn() {
      return this.fadeIn;
   }

   public int getStay() {
      return this.stay;
   }

   public int getFadeOut() {
      return this.fadeOut;
   }
}
