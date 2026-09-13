package net.minecraft.network.protocol.login;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.RegistryOps;

public record ClientboundLoginDisconnectPacket(Component reason) implements Packet<ClientLoginPacketListener> {
   private static final RegistryOps<JsonElement> OPS = RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE);
   public static final StreamCodec<ByteBuf, ClientboundLoginDisconnectPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.lenientJson(262144).apply(ByteBufCodecs.fromCodec(OPS, ComponentSerialization.CODEC)),
      ClientboundLoginDisconnectPacket::reason,
      ClientboundLoginDisconnectPacket::new
   );

   @Override
   public PacketType<ClientboundLoginDisconnectPacket> type() {
      return LoginPacketTypes.CLIENTBOUND_LOGIN_DISCONNECT;
   }

   public void handle(ClientLoginPacketListener p_134818_) {
      p_134818_.handleDisconnect(this);
   }
}
