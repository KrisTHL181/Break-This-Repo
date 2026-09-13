package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ParticleEngine {
   private static final List<ParticleRenderType> RENDER_ORDER = List.of(
      ParticleRenderType.SINGLE_QUADS, ParticleRenderType.ITEM_PICKUP, ParticleRenderType.ELDER_GUARDIANS
   );
   protected ClientLevel level;
   private final Map<ParticleRenderType, ParticleGroup<?>> particles = Maps.newIdentityHashMap();
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
   private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap();
   private final ParticleResources resourceManager;
   private final RandomSource random = RandomSource.create();

   public ParticleEngine(ClientLevel p_107299_, ParticleResources p_423228_) {
      this.level = p_107299_;
      this.resourceManager = p_423228_;
   }

   public void createTrackingEmitter(Entity p_107330_, ParticleOptions p_107331_) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, p_107330_, p_107331_));
   }

   public void createTrackingEmitter(Entity p_107333_, ParticleOptions p_107334_, int p_107335_) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, p_107333_, p_107334_, p_107335_));
   }

   public @Nullable Particle createParticle(
      ParticleOptions p_107371_, double p_107372_, double p_107373_, double p_107374_, double p_107375_, double p_107376_, double p_107377_
   ) {
      Particle particle = this.makeParticle(p_107371_, p_107372_, p_107373_, p_107374_, p_107375_, p_107376_, p_107377_);
      if (particle != null) {
         this.add(particle);
         return particle;
      } else {
         return null;
      }
   }

   private <T extends ParticleOptions> @Nullable Particle makeParticle(
      T p_107396_, double p_107397_, double p_107398_, double p_107399_, double p_107400_, double p_107401_, double p_107402_
   ) {
      ParticleProvider<T> particleprovider = (ParticleProvider<T>)this.resourceManager
         .getProviders()
         .get(BuiltInRegistries.PARTICLE_TYPE.getId(p_107396_.getType()));
      return particleprovider == null
         ? null
         : particleprovider.createParticle(p_107396_, this.level, p_107397_, p_107398_, p_107399_, p_107400_, p_107401_, p_107402_, this.random);
   }

   public void add(Particle p_107345_) {
      Optional<ParticleLimit> optional = p_107345_.getParticleLimit();
      if (optional.isPresent()) {
         if (this.hasSpaceInParticleLimit(optional.get())) {
            this.particlesToAdd.add(p_107345_);
            this.updateCount(optional.get(), 1);
         }
      } else {
         this.particlesToAdd.add(p_107345_);
      }
   }

   public void tick() {
      this.particles.forEach((p_420862_, p_420863_) -> {
         Profiler.get().push(p_420862_.name());
         p_420863_.tickParticles();
         Profiler.get().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         List<TrackingEmitter> list = Lists.newArrayList();

         for (TrackingEmitter trackingemitter : this.trackingEmitters) {
            trackingemitter.tick();
            if (!trackingemitter.isAlive()) {
               list.add(trackingemitter);
            }
         }

         this.trackingEmitters.removeAll(list);
      }

      Particle particle;
      if (!this.particlesToAdd.isEmpty()) {
         while ((particle = this.particlesToAdd.poll()) != null) {
            this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle);
         }
      }
   }

   private ParticleGroup<?> createParticleGroup(ParticleRenderType p_428647_) {
      if (p_428647_ == ParticleRenderType.ITEM_PICKUP) {
         return new ItemPickupParticleGroup(this);
      } else if (p_428647_ == ParticleRenderType.ELDER_GUARDIANS) {
         return new ElderGuardianParticleGroup(this);
      } else {
         return p_428647_ == ParticleRenderType.NO_RENDER ? new NoRenderParticleGroup(this) : new QuadParticleGroup(this, p_428647_);
      }
   }

   protected void updateCount(ParticleLimit p_423291_, int p_172283_) {
      this.trackedParticleCounts.addTo(p_423291_, p_172283_);
   }

   public void extract(ParticlesRenderState p_423938_, Frustum p_424803_, Camera p_430521_, float p_426823_) {
      for (ParticleRenderType particlerendertype : RENDER_ORDER) {
         ParticleGroup<?> particlegroup = this.particles.get(particlerendertype);
         if (particlegroup != null && !particlegroup.isEmpty()) {
            p_423938_.add(particlegroup.extractRenderState(p_424803_, p_430521_, p_426823_));
         }
      }
   }

   public void setLevel(@Nullable ClientLevel p_107343_) {
      this.level = p_107343_;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(ParticleGroup::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleLimit p_426844_) {
      return this.trackedParticleCounts.getInt(p_426844_) < p_426844_.limit();
   }

   public void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }
}
