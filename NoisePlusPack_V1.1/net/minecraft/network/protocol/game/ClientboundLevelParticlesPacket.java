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
      T p_132292_,
      boolean p_132293_,
      boolean p_378159_,
      double p_132294_,
      double p_132295_,
      double p_132296_,
      float p_132297_,
      float p_132298_,
      float p_132299_,
      float p_132300_,
      int p_132301_
   ) {
      this.particle = p_132292_;
      this.overrideLimiter = p_132293_;
      this.alwaysShow = p_378159_;
      this.x = p_132294_;
      this.y = p_132295_;
      this.z = p_132296_;
      this.xDist = p_132297_;
      this.yDist = p_132298_;
      this.zDist = p_132299_;
      this.maxSpeed = p_132300_;
      this.count = p_132301_;
   }

   private ClientboundLevelParticlesPacket(RegistryFriendlyByteBuf p_334002_) {
      this.overrideLimiter = p_334002_.readBoolean();
      this.alwaysShow = p_334002_.readBoolean();
      this.x = p_334002_.readDouble();
      this.y = p_334002_.readDouble();
      this.z = p_334002_.readDouble();
      this.xDist = p_334002_.readFloat();
      this.yDist = p_334002_.readFloat();
      this.zDist = p_334002_.readFloat();
      this.maxSpeed = p_334002_.readFloat();
      this.count = p_334002_.readInt();
      this.particle = ParticleTypes.STREAM_CODEC.decode(p_334002_);
   }

   private void write(RegistryFriendlyByteBuf p_335688_) {
      p_335688_.writeBoolean(this.overrideLimiter);
      p_335688_.writeBoolean(this.alwaysShow);
      p_335688_.writeDouble(this.x);
      p_335688_.writeDouble(this.y);
      p_335688_.writeDouble(this.z);
      p_335688_.writeFloat(this.xDist);
      p_335688_.writeFloat(this.yDist);
      p_335688_.writeFloat(this.zDist);
      p_335688_.writeFloat(this.maxSpeed);
      p_335688_.writeInt(this.count);
      ParticleTypes.STREAM_CODEC.encode(p_335688_, this.particle);
   }

   @Override
   public PacketType<ClientboundLevelParticlesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES;
   }

   public void handle(ClientGamePacketListener p_132310_) {
      p_132310_.handleParticleEvent(this);
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
