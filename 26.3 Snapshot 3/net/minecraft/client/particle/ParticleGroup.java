package net.minecraft.client.particle;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

public abstract class ParticleGroup<P extends Particle> {
   private static final int MAX_PARTICLES = 16384;
   private static final int RESERVOIR_SIZE = 4096;
   private static final int RESERVOIR_START = 12288;
   protected final ParticleEngine engine;
   protected final Queue<P> particles = new ArrayDeque<>(16384);

   public ParticleGroup(final ParticleEngine engine) {
      this.engine = engine;
   }

   public boolean isEmpty() {
      return this.particles.isEmpty();
   }

   public void tickParticles() {
      if (!this.particles.isEmpty()) {
         Iterator<P> iterator = this.particles.iterator();

         while (iterator.hasNext()) {
            P particle = iterator.next();
            this.tickParticle(particle);
            if (!particle.isAlive()) {
               particle.getParticleLimit().ifPresent(options -> this.engine.updateCount(options, -1));
               iterator.remove();
            }
         }
      }
   }

   private void tickParticle(final Particle particle) {
      try {
         particle.tick();
      } catch (Throwable t) {
         CrashReport report = CrashReport.forThrowable(t, "Ticking Particle");
         CrashReportCategory category = report.addCategory("Particle being ticked");
         category.setDetail("Particle", particle::toString);
         category.setDetail("Particle Type", particle.getGroup()::toString);
         throw new ReportedException(report);
      }
   }

   public boolean add(final Particle particle) {
      int currentSize = this.particles.size();
      if (currentSize >= 16384) {
         return false;
      }

      if (currentSize >= 12288) {
         float freeSpace = (16384 - currentSize) / 4096.0F;
         if (this.engine.getRandom().nextFloat() >= freeSpace * freeSpace) {
            return false;
         }
      }

      this.particles.add((P)particle);
      return true;
   }

   public int size() {
      return this.particles.size();
   }

   public abstract ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime);
}
