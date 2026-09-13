package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderLerpSizePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderLerpSizePacket> STREAM_CODEC = Packet.codec(
      ClientboundSetBorderLerpSizePacket::write, ClientboundSetBorderLerpSizePacket::new
   );
   private final double oldSize;
   private final double newSize;
   private final long lerpTime;

   public ClientboundSetBorderLerpSizePacket(WorldBorder p_179229_) {
      this.oldSize = p_179229_.getSize();
      this.newSize = p_179229_.getLerpTarget();
      this.lerpTime = p_179229_.getLerpTime();
   }

   private ClientboundSetBorderLerpSizePacket(FriendlyByteBuf p_179231_) {
      this.oldSize = p_179231_.readDouble();
      this.newSize = p_179231_.readDouble();
      this.lerpTime = p_179231_.readVarLong();
   }

   private void write(FriendlyByteBuf p_179233_) {
      p_179233_.writeDouble(this.oldSize);
      p_179233_.writeDouble(this.newSize);
      p_179233_.writeVarLong(this.lerpTime);
   }

   @Override
   public PacketType<ClientboundSetBorderLerpSizePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_BORDER_LERP_SIZE;
   }

   public void handle(ClientGamePacketListener p_179237_) {
      p_179237_.handleSetBorderLerpSize(this);
   }

   public double getOldSize() {
      return this.oldSize;
   }

   public double getNewSize() {
      return this.newSize;
   }

   public long getLerpTime() {
      return this.lerpTime;
   }
}
