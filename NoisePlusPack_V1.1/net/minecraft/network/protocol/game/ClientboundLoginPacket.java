package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ClientboundLoginPacket(
   int playerId,
   boolean hardcore,
   Set<ResourceKey<Level>> levels,
   int maxPlayers,
   int chunkRadius,
   int simulationDistance,
   boolean reducedDebugInfo,
   boolean showDeathScreen,
   boolean doLimitedCrafting,
   CommonPlayerSpawnInfo commonPlayerSpawnInfo,
   boolean enforcesSecureChat
) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLoginPacket> STREAM_CODEC = Packet.codec(
      ClientboundLoginPacket::write, ClientboundLoginPacket::new
   );

   private ClientboundLoginPacket(RegistryFriendlyByteBuf p_333088_) {
      this(
         p_333088_.readInt(),
         p_333088_.readBoolean(),
         p_333088_.readCollection(Sets::newHashSetWithExpectedSize, p_258210_ -> p_258210_.readResourceKey(Registries.DIMENSION)),
         p_333088_.readVarInt(),
         p_333088_.readVarInt(),
         p_333088_.readVarInt(),
         p_333088_.readBoolean(),
         p_333088_.readBoolean(),
         p_333088_.readBoolean(),
         new CommonPlayerSpawnInfo(p_333088_),
         p_333088_.readBoolean()
      );
   }

   private void write(RegistryFriendlyByteBuf p_329645_) {
      p_329645_.writeInt(this.playerId);
      p_329645_.writeBoolean(this.hardcore);
      p_329645_.writeCollection(this.levels, FriendlyByteBuf::writeResourceKey);
      p_329645_.writeVarInt(this.maxPlayers);
      p_329645_.writeVarInt(this.chunkRadius);
      p_329645_.writeVarInt(this.simulationDistance);
      p_329645_.writeBoolean(this.reducedDebugInfo);
      p_329645_.writeBoolean(this.showDeathScreen);
      p_329645_.writeBoolean(this.doLimitedCrafting);
      this.commonPlayerSpawnInfo.write(p_329645_);
      p_329645_.writeBoolean(this.enforcesSecureChat);
   }

   @Override
   public PacketType<ClientboundLoginPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LOGIN;
   }

   public void handle(ClientGamePacketListener p_132397_) {
      p_132397_.handleLogin(this);
   }
}
