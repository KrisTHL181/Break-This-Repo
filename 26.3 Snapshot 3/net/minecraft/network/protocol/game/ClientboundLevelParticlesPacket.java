package net.minecraft.network.protocol.game;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLevelParticlesPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelParticlesPacket> STREAM_CODEC = Packet.codec(
      ClientboundLevelParticlesPacket::write, ClientboundLevelParticlesPacket::new
   );
   private final double x;
   private final double y;
   private final double z;
   private final float xDist;
   private final float yDist;
   private final float zDist;
   private final float maxSpeed;
   private final int count;
   private final boolean overrideLimiter;
   private final boolean alwaysShow;
   private final ParticleOptions particle;

   public <T extends ParticleOptions> ClientboundLevelParticlesPacket(
      final T particle,
      final boolean overrideLimiter,
      final boolean alwaysShow,
      final double x,
      final double y,
      final double z,
      final float xDist,
      final float yDist,
      final float zDist,
      final float maxSpeed,
      final int count
   ) {
      this.particle = particle;
      this.overrideLimiter = overrideLimiter;
      this.alwaysShow = alwaysShow;
      this.x = x;
      this.y = y;
      this.z = z;
      this.xDist = xDist;
      this.yDist = yDist;
      this.zDist = zDist;
      this.maxSpeed = maxSpeed;
      this.count = count;
   }

   private ClientboundLevelParticlesPacket(final RegistryFriendlyByteBuf input) {
      this.overrideLimiter = input.readBoolean();
      this.alwaysShow = input.readBoolean();
      this.x = input.readDouble();
      this.y = input.readDouble();
      this.z = input.readDouble();
      this.xDist = input.readFloat();
      this.yDist = input.readFloat();
      this.zDist = input.readFloat();
      this.maxSpeed = input.readFloat();
      this.count = input.readInt();
      this.particle = ParticleTypes.STREAM_CODEC.decode(input);
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeBoolean(this.overrideLimiter);
      output.writeBoolean(this.alwaysShow);
      output.writeDouble(this.x);
      output.writeDouble(this.y);
      output.writeDouble(this.z);
      output.writeFloat(this.xDist);
      output.writeFloat(this.yDist);
      output.writeFloat(this.zDist);
      output.writeFloat(this.maxSpeed);
      output.writeInt(this.count);
      ParticleTypes.STREAM_CODEC.encode(output, this.particle);
   }

   @Override
   public PacketType<ClientboundLevelParticlesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleParticleEvent(this);
   }

   public boolean isOverrideLimiter() {
      return this.overrideLimiter;
   }

   public boolean alwaysShow() {
      return this.alwaysShow;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getXDist() {
      return this.xDist;
   }

   public float getYDist() {
      return this.yDist;
   }

   public float getZDist() {
      return this.zDist;
   }

   public float getMaxSpeed() {
      return this.maxSpeed;
   }

   public int getCount() {
      return this.count;
   }

   public ParticleOptions getParticle() {
      return this.particle;
   }
}
