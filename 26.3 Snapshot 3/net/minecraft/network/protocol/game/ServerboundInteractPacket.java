package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public record ServerboundInteractPacket(int entityId, InteractionHand hand, Vec3 location, boolean usingSecondaryAction)
   implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundInteractPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ServerboundInteractPacket::entityId,
      InteractionHand.STREAM_CODEC,
      ServerboundInteractPacket::hand,
      Vec3.LP_STREAM_CODEC,
      ServerboundInteractPacket::location,
      ByteBufCodecs.BOOL,
      ServerboundInteractPacket::usingSecondaryAction,
      ServerboundInteractPacket::new
   );

   @Override
   public PacketType<ServerboundInteractPacket> type() {
      return GamePacketTypes.SERVERBOUND_INTERACT;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleInteract(this);
   }
}
