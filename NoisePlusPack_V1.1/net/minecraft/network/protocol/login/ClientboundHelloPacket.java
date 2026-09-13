package net.minecraft.network.protocol.login;

import java.security.PublicKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ClientboundHelloPacket implements Packet<ClientLoginPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundHelloPacket> STREAM_CODEC = Packet.codec(
      ClientboundHelloPacket::write, ClientboundHelloPacket::new
   );
   private final String serverId;
   private final byte[] publicKey;
   private final byte[] challenge;
   private final boolean shouldAuthenticate;

   public ClientboundHelloPacket(String p_134782_, byte[] p_134783_, byte[] p_134784_, boolean p_331026_) {
      this.serverId = p_134782_;
      this.publicKey = p_134783_;
      this.challenge = p_134784_;
      this.shouldAuthenticate = p_331026_;
   }

   private ClientboundHelloPacket(FriendlyByteBuf p_179816_) {
      this.serverId = p_179816_.readUtf(20);
      this.publicKey = p_179816_.readByteArray();
      this.challenge = p_179816_.readByteArray();
      this.shouldAuthenticate = p_179816_.readBoolean();
   }

   private void write(FriendlyByteBuf p_134793_) {
      p_134793_.writeUtf(this.serverId);
      p_134793_.writeByteArray(this.publicKey);
      p_134793_.writeByteArray(this.challenge);
      p_134793_.writeBoolean(this.shouldAuthenticate);
   }

   @Override
   public PacketType<ClientboundHelloPacket> type() {
      return LoginPacketTypes.CLIENTBOUND_HELLO;
   }

   public void handle(ClientLoginPacketListener p_134790_) {
      p_134790_.handleHello(this);
   }

   public String getServerId() {
      return this.serverId;
   }

   public PublicKey getPublicKey() throws CryptException {
      return Crypt.byteToPublicKey(this.publicKey);
   }

   public byte[] getChallenge() {
      return this.challenge;
   }

   public boolean shouldAuthenticate() {
      return this.shouldAuthenticate;
   }
}
