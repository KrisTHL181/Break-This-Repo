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

   public ClientboundHelloPacket(final String serverId, final byte[] publicKey, final byte[] challenge, final boolean shouldAuthenticate) {
      this.serverId = serverId;
      this.publicKey = publicKey;
      this.challenge = challenge;
      this.shouldAuthenticate = shouldAuthenticate;
   }

   private ClientboundHelloPacket(final FriendlyByteBuf input) {
      this.serverId = input.readUtf(20);
      this.publicKey = input.readByteArray();
      this.challenge = input.readByteArray();
      this.shouldAuthenticate = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeUtf(this.serverId);
      output.writeByteArray(this.publicKey);
      output.writeByteArray(this.challenge);
      output.writeBoolean(this.shouldAuthenticate);
   }

   @Override
   public PacketType<ClientboundHelloPacket> type() {
      return LoginPacketTypes.CLIENTBOUND_HELLO;
   }

   public void handle(final ClientLoginPacketListener listener) {
      listener.handleHello(this);
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
