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

   private ClientboundTickingStepPacket(final FriendlyByteBuf input) {
      this(input.readVarInt());
   }

   public static ClientboundTickingStepPacket from(final TickRateManager manager) {
      return new ClientboundTickingStepPacket(manager.frozenTicksToRun());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.tickSteps);
   }

   @Override
   public PacketType<ClientboundTickingStepPacket> type() {
      return GamePacketTypes.CLIENTBOUND_TICKING_STEP;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleTickingStep(this);
   }
}
