package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class ClientboundSoundEntityPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSoundEntityPacket> STREAM_CODEC = Packet.codec(
      ClientboundSoundEntityPacket::write, ClientboundSoundEntityPacket::new
   );
   private final Holder<SoundEvent> sound;
   private final SoundSource source;
   private final int id;
   private final float volume;
   private final float pitch;
   private final long seed;

   public ClientboundSoundEntityPacket(
      final Holder<SoundEvent> sound, final SoundSource source, final Entity sourceEntity, final float volume, final float pitch, final long seed
   ) {
      this.sound = sound;
      this.source = source;
      this.id = sourceEntity.getId();
      this.volume = volume;
      this.pitch = pitch;
      this.seed = seed;
   }

   private ClientboundSoundEntityPacket(final RegistryFriendlyByteBuf input) {
      this.sound = SoundEvent.STREAM_CODEC.decode(input);
      this.source = input.readEnum(SoundSource.class);
      this.id = input.readVarInt();
      this.volume = input.readFloat();
      this.pitch = input.readFloat();
      this.seed = input.readLong();
   }

   private void write(final RegistryFriendlyByteBuf output) {
      SoundEvent.STREAM_CODEC.encode(output, this.sound);
      output.writeEnum(this.source);
      output.writeVarInt(this.id);
      output.writeFloat(this.volume);
      output.writeFloat(this.pitch);
      output.writeLong(this.seed);
   }

   @Override
   public PacketType<ClientboundSoundEntityPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SOUND_ENTITY;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSoundEntityEvent(this);
   }

   public Holder<SoundEvent> getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public int getId() {
      return this.id;
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public long getSeed() {
      return this.seed;
   }
}
