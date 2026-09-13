package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundKeyPacket> STREAM_CODEC = Packet.codec(ServerboundKeyPacket::write, ServerboundKeyPacket::new);
   private final byte[] keybytes;
   private final byte[] encryptedChallenge;

   public ServerboundKeyPacket(SecretKey p_134856_, PublicKey p_134857_, byte[] p_134858_) throws CryptException {
      this.keybytes = Crypt.encryptUsingKey(p_134857_, p_134856_.getEncoded());
      this.encryptedChallenge = Crypt.encryptUsingKey(p_134857_, p_134858_);
   }

   private ServerboundKeyPacket(FriendlyByteBuf p_179829_) {
      this.keybytes = p_179829_.readByteArray();
      this.encryptedChallenge = p_179829_.readByteArray();
   }

   private void write(FriendlyByteBuf p_134870_) {
      p_134870_.writeByteArray(this.keybytes);
      p_134870_.writeByteArray(this.encryptedChallenge);
   }

   @Override
   public PacketType<ServerboundKeyPacket> type() {
      return LoginPacketTypes.SERVERBOUND_KEY;
   }

   public void handle(ServerLoginPacketListener p_134866_) {
      p_134866_.handleKey(this);
   }

   public SecretKey getSecretKey(PrivateKey p_134860_) throws CryptException {
      return Crypt.decryptByteToSecretKey(p_134860_, this.keybytes);
   }

   public boolean isChallengeValid(byte[] p_254210_, PrivateKey p_253763_) {
      try {
         return Arrays.equals(p_254210_, Crypt.decryptUsingKey(p_253763_, this.encryptedChallenge));
      } catch (CryptException cryptexception) {
         return false;
      }
   }
}
