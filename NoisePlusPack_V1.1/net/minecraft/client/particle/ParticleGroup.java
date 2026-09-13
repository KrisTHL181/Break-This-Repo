package net.minecraft.client.particle;

import com.google.common.collect.EvictingQueue;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ParticleGroup<P extends Particle> {
   private static final int MAX_PARTICLES = 16384;
   protected final ParticleEngine engine;
   protected final Queue<P> particles = EvictingQueue.create(16384);

   public ParticleGroup(ParticleEngine p_427246_) {
      this.engine = p_427246_;
   }

   public boolean isEmpty() {
      return this.particles.isEmpty();
   }

   public void tickParticles() {
      if (!this.particles.isEmpty()) {
         Iterator<P> iterator = this.particles.iterator();

         while (iterator.hasNext()) {
            P p = iterator.next();
            this.tickParticle(p);
            if (!p.isAlive()) {
               p.getParticleLimit().ifPresent(p_427352_ -> this.engine.updateCount(p_427352_, -1));
               iterator.remove();
            }
         }
      }
   }

   private void tickParticle(Particle p_423907_) {
      try {
         p_423907_.tick();
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking Particle");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being ticked");
         crashreportcategory.setDetail("Particle", p_423907_::toString);
         crashreportcategory.setDetail("Particle Type", p_423907_.getGroup()::toString);
         throw new ReportedException(crashreport);
      }
   }

   public void add(Particle p_425439_) {
      this.particles.add((P)p_425439_);
   }

   public int size() {
      return this.particles.size();
   }

   public abstract ParticleGroupRenderState extractRenderState(Frustum var1, Camera var2, float var3);

   public Queue<P> getAll() {
      return this.particles;
   }
}
