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

   public ClientboundSetDisplayObjectivePacket(DisplaySlot p_301315_, @Nullable Objective p_133132_) {
      this.slot = p_301315_;
      if (p_133132_ == null) {
         this.objectiveName = "";
      } else {
         this.objectiveName = p_133132_.getName();
      }
   }

   private ClientboundSetDisplayObjectivePacket(FriendlyByteBuf p_179288_) {
      this.slot = p_179288_.readById(DisplaySlot.BY_ID);
      this.objectiveName = p_179288_.readUtf();
   }

   private void write(FriendlyByteBuf p_133141_) {
      p_133141_.writeById(DisplaySlot::id, this.slot);
      p_133141_.writeUtf(this.objectiveName);
   }

   @Override
   public PacketType<ClientboundSetDisplayObjectivePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_DISPLAY_OBJECTIVE;
   }

   public void handle(ClientGamePacketListener p_133138_) {
      p_133138_.handleSetDisplayObjective(this);
   }

   public DisplaySlot getSlot() {
      return this.slot;
   }

   public @Nullable String getObjectiveName() {
      return Objects.equals(this.objectiveName, "") ? null : this.objectiveName;
   }
}
