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

   public ClientboundSetTitlesAnimationPacket(int p_179404_, int p_179405_, int p_179406_) {
      this.fadeIn = p_179404_;
      this.stay = p_179405_;
      this.fadeOut = p_179406_;
   }

   private ClientboundSetTitlesAnimationPacket(FriendlyByteBuf p_179408_) {
      this.fadeIn = p_179408_.readInt();
      this.stay = p_179408_.readInt();
      this.fadeOut = p_179408_.readInt();
   }

   private void write(FriendlyByteBuf p_179410_) {
      p_179410_.writeInt(this.fadeIn);
      p_179410_.writeInt(this.stay);
      p_179410_.writeInt(this.fadeOut);
   }

   @Override
   public PacketType<ClientboundSetTitlesAnimationPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_TITLES_ANIMATION;
   }

   public void handle(ClientGamePacketListener p_179414_) {
      p_179414_.setTitlesAnimation(this);
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
