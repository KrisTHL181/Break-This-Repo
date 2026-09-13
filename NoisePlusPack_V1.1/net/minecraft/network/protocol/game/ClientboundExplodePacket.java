package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.phys.Vec3;

public record ClientboundExplodePacket(
   Vec3 center,
   float radius,
   int blockCount,
   Optional<Vec3> playerKnockback,
   ParticleOptions explosionParticle,
   Holder<SoundEvent> explosionSound,
   WeightedList<ExplosionParticleInfo> blockParticles
) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundExplodePacket> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC,
      ClientboundExplodePacket::center,
      ByteBufCodecs.FLOAT,
      ClientboundExplodePacket::radius,
      ByteBufCodecs.INT,
      ClientboundExplodePacket::blockCount,
      Vec3.STREAM_CODEC.apply(ByteBufCodecs::optional),
      ClientboundExplodePacket::playerKnockback,
      ParticleTypes.STREAM_CODEC,
      ClientboundExplodePacket::explosionParticle,
      SoundEvent.STREAM_CODEC,
      ClientboundExplodePacket::explosionSound,
      WeightedList.streamCodec(ExplosionParticleInfo.STREAM_CODEC),
      ClientboundExplodePacket::blockParticles,
      ClientboundExplodePacket::new
   );

   @Override
   public PacketType<ClientboundExplodePacket> type() {
      return GamePacketTypes.CLIENTBOUND_EXPLODE;
   }

   public void handle(ClientGamePacketListener p_132126_) {
      p_132126_.handleExplosion(this);
   }
}
