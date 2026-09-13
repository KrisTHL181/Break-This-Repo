package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ClientboundDamageEventPacket(int entityId, Holder<DamageType> sourceType, int sourceCauseId, int sourceDirectId, Optional<Vec3> sourcePosition)
   implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDamageEventPacket> STREAM_CODEC = Packet.codec(
      ClientboundDamageEventPacket::write, ClientboundDamageEventPacket::new
   );

   public ClientboundDamageEventPacket(final Entity entity, final DamageSource source) {
      this(
         entity.getId(),
         source.typeHolder(),
         source.getEntity() != null ? source.getEntity().getId() : -1,
         source.getDirectEntity() != null ? source.getDirectEntity().getId() : -1,
         Optional.ofNullable(source.sourcePositionRaw())
      );
   }

   private ClientboundDamageEventPacket(final RegistryFriendlyByteBuf input) {
      this(
         input.readVarInt(),
         DamageType.STREAM_CODEC.decode(input),
         readOptionalEntityId(input),
         readOptionalEntityId(input),
         input.readOptional(i -> new Vec3(i.readDouble(), i.readDouble(), i.readDouble()))
      );
   }

   private static void writeOptionalEntityId(final FriendlyByteBuf output, final int id) {
      output.writeVarInt(id + 1);
   }

   private static int readOptionalEntityId(final FriendlyByteBuf input) {
      return input.readVarInt() - 1;
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeVarInt(this.entityId);
      DamageType.STREAM_CODEC.encode(output, this.sourceType);
      writeOptionalEntityId(output, this.sourceCauseId);
      writeOptionalEntityId(output, this.sourceDirectId);
      output.writeOptional(this.sourcePosition, (o, pos) -> {
         o.writeDouble(pos.x());
         o.writeDouble(pos.y());
         o.writeDouble(pos.z());
      });
   }

   @Override
   public PacketType<ClientboundDamageEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_DAMAGE_EVENT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleDamageEvent(this);
   }

   public DamageSource getSource(final Level level) {
      if (this.sourcePosition.isPresent()) {
         return new DamageSource(this.sourceType, this.sourcePosition.get());
      }

      Entity cause = level.getEntity(this.sourceCauseId);
      Entity direct = level.getEntity(this.sourceDirectId);
      return new DamageSource(this.sourceType, direct, cause);
   }
}
