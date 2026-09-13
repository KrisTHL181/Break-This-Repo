package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatEndPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerCombatEndPacket> STREAM_CODEC = Packet.codec(
      ClientboundPlayerCombatEndPacket::write, ClientboundPlayerCombatEndPacket::new
   );
   private final int duration;

   public ClientboundPlayerCombatEndPacket(CombatTracker p_179040_) {
      this(p_179040_.getCombatDuration());
   }

   public ClientboundPlayerCombatEndPacket(int p_289544_) {
      this.duration = p_289544_;
   }

   private ClientboundPlayerCombatEndPacket(FriendlyByteBuf p_179042_) {
      this.duration = p_179042_.readVarInt();
   }

   private void write(FriendlyByteBuf p_179044_) {
      p_179044_.writeVarInt(this.duration);
   }

   @Override
   public PacketType<ClientboundPlayerCombatEndPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_END;
   }

   public void handle(ClientGamePacketListener p_179048_) {
      p_179048_.handlePlayerCombatEnd(this);
   }
}
