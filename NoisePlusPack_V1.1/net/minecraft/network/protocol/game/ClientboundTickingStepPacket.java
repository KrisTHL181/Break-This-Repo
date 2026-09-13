package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStepPacket(int tickSteps) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundTickingStepPacket> STREAM_CODEC = Packet.codec(
      ClientboundTickingStepPacket::write, ClientboundTickingStepPacket::new
   );

   private ClientboundTickingStepPacket(FriendlyByteBuf p_311037_) {
      this(p_311037_.readVarInt());
   }

   public static ClientboundTickingStepPacket from(TickRateManager p_312211_) {
      return new ClientboundTickingStepPacket(p_312211_.frozenTicksToRun());
   }

   private void write(FriendlyByteBuf p_311017_) {
      p_311017_.writeVarInt(this.tickSteps);
   }

   @Override
   public PacketType<ClientboundTickingStepPacket> type() {
      return GamePacketTypes.CLIENTBOUND_TICKING_STEP;
   }

   public void handle(ClientGamePacketListener p_309817_) {
      p_309817_.handleTickingStep(this);
   }
}
