package net.minecraft.network.protocol.game;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import org.jspecify.annotations.Nullable;

public class ClientboundSetDisplayObjectivePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetDisplayObjectivePacket> STREAM_CODEC = Packet.codec(
      ClientboundSetDisplayObjectivePacket::write, ClientboundSetDisplayObjectivePacket::new
   );
   private final DisplaySlot slot;
   private final String objectiveName;

   public ClientboundSetDisplayObjectivePacket(final DisplaySlot slot, final @Nullable Objective objective) {
      this.slot = slot;
      if (objective == null) {
         this.objectiveName = "";
      } else {
         this.objectiveName = objective.getName();
      }
   }

   private ClientboundSetDisplayObjectivePacket(final FriendlyByteBuf input) {
      this.slot = input.readById(DisplaySlot.BY_ID);
      this.objectiveName = input.readUtf();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeById(DisplaySlot::id, this.slot);
      output.writeUtf(this.objectiveName);
   }

   @Override
   public PacketType<ClientboundSetDisplayObjectivePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_DISPLAY_OBJECTIVE;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetDisplayObjective(this);
   }

   public DisplaySlot getSlot() {
      return this.slot;
   }

   public @Nullable String getObjectiveName() {
      return Objects.equals(this.objectiveName, "") ? null : this.objectiveName;
   }
}
