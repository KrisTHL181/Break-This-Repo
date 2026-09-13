package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TrackingEmitter extends NoRenderParticle {
   private final Entity entity;
   private int life;
   private final int lifeTime;
   private final ParticleOptions particleType;

   public TrackingEmitter(final ClientLevel level, final Entity entity, final ParticleOptions particleType) {
      this(level, entity, particleType, 3);
   }

   public TrackingEmitter(final ClientLevel level, final Entity entity, final ParticleOptions particleType, final int lifeTime) {
      this(level, entity, particleType, lifeTime, entity.getDeltaMovement());
   }

   private TrackingEmitter(final ClientLevel level, final Entity entity, final ParticleOptions particleType, final int lifeTime, final Vec3 movement) {
      super(level, entity.getX(), entity.getY(0.5), entity.getZ(), movement.x, movement.y, movement.z);
      this.entity = entity;
      this.lifeTime = lifeTime;
      this.particleType = particleType;
      this.tick();
   }

   @Override
   public void tick() {
      for (int i = 0; i < 16; i++) {
         double xa = this.random.nextFloat() * 2.0F - 1.0F;
         double ya = this.random.nextFloat() * 2.0F - 1.0F;
         double za = this.random.nextFloat() * 2.0F - 1.0F;
         if (!(xa * xa + ya * ya + za * za > 1.0)) {
            double x = this.entity.getX(xa / 4.0);
            double y = this.entity.getY(0.5 + ya / 4.0);
            double z = this.entity.getZ(za / 4.0);
            this.level.addParticle(this.particleType, x, y, z, xa, ya + 0.2, za);
         }
      }

      this.life++;
      if (this.life >= this.lifeTime) {
         this.remove();
      }
   }
}
