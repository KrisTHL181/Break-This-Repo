package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ClientboundSoundPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSoundPacket> STREAM_CODEC = Packet.codec(
      ClientboundSoundPacket::write, ClientboundSoundPacket::new
   );
   public static final float LOCATION_ACCURACY = 8.0F;
   private final Holder<SoundEvent> sound;
   private final SoundSource source;
   private final int x;
   private final int y;
   private final int z;
   private final float volume;
   private final float pitch;
   private final long seed;

   public ClientboundSoundPacket(
      final Holder<SoundEvent> sound,
      final SoundSource source,
      final double x,
      final double y,
      final double z,
      final float volume,
      final float pitch,
      final long seed
   ) {
      this.sound = sound;
      this.source = source;
      this.x = (int)(x * 8.0);
      this.y = (int)(y * 8.0);
      this.z = (int)(z * 8.0);
      this.volume = volume;
      this.pitch = pitch;
      this.seed = seed;
   }

   private ClientboundSoundPacket(final RegistryFriendlyByteBuf input) {
      this.sound = SoundEvent.STREAM_CODEC.decode(input);
      this.source = input.readEnum(SoundSource.class);
      this.x = input.readInt();
      this.y = input.readInt();
      this.z = input.readInt();
      this.volume = input.readFloat();
      this.pitch = input.readFloat();
      this.seed = input.readLong();
   }

   private void write(final RegistryFriendlyByteBuf output) {
      SoundEvent.STREAM_CODEC.encode(output, this.sound);
      output.writeEnum(this.source);
      output.writeInt(this.x);
      output.writeInt(this.y);
      output.writeInt(this.z);
      output.writeFloat(this.volume);
      output.writeFloat(this.pitch);
      output.writeLong(this.seed);
   }

   @Override
   public PacketType<ClientboundSoundPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SOUND;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSoundEvent(this);
   }

   public Holder<SoundEvent> getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public double getX() {
      return this.x / 8.0F;
   }

   public double getY() {
      return this.y / 8.0F;
   }

   public double getZ() {
      return this.z / 8.0F;
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
