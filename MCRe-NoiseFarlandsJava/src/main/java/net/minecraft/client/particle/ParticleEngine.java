package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Map.Entry;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
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
    private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap<>();
    private final ParticleResources resourceManager;
    private final RandomSource random = RandomSource.create();

    public ParticleEngine(final ClientLevel level, final ParticleResources resourceManager) {
        this.level = level;
        this.resourceManager = resourceManager;
    }

    public void createTrackingEmitter(final Entity entity, final ParticleOptions particle) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particle));
    }

    public void createTrackingEmitter(final Entity entity, final ParticleOptions particle, final int lifeTime) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particle, lifeTime));
    }

    public @Nullable Particle createParticle(
        final ParticleOptions options, final double x, final double y, final double z, final double xa, final double ya, final double za
    ) {
        Particle particle = this.makeParticle(options, x, y, z, xa, ya, za);
        if (particle != null) {
            this.add(particle);
            return particle;
        } else {
            return null;
        }
    }

    private <T extends ParticleOptions> @Nullable Particle makeParticle(
        final T options, final double x, final double y, final double z, final double xa, final double ya, final double za
    ) {
        ParticleProvider<T> provider = (ParticleProvider<T>)this.resourceManager.getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getId(options.getType()));
        return provider == null ? null : provider.createParticle(options, this.level, x, y, z, xa, ya, za, this.random);
    }

    public void add(final Particle p) {
        Optional<ParticleLimit> limit = p.getParticleLimit();
        if (limit.isPresent()) {
            if (this.hasSpaceInParticleLimit(limit.get())) {
                this.particlesToAdd.add(p);
                this.updateCount(limit.get(), 1);
            }
        } else {
            this.particlesToAdd.add(p);
        }
    }

    public void tick() {
        this.particles.forEach((type, group) -> {
            Profiler.get().push(type.name());
            group.tickParticles();
            Profiler.get().pop();
        });
        if (!this.trackingEmitters.isEmpty()) {
            List<TrackingEmitter> removed = Lists.newArrayList();

            for (TrackingEmitter emitter : this.trackingEmitters) {
                emitter.tick();
                if (!emitter.isAlive()) {
                    removed.add(emitter);
                }
            }

            this.trackingEmitters.removeAll(removed);
        }

        Particle particle;
        if (!this.particlesToAdd.isEmpty()) {
            while ((particle = this.particlesToAdd.poll()) != null) {
                if (!this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle)) {
                    particle.getParticleLimit().ifPresent(options -> this.updateCount(options, -1));
                }
            }
        }
    }

    private ParticleGroup<?> createParticleGroup(final ParticleRenderType type) {
        if (type == ParticleRenderType.ITEM_PICKUP) {
            return new ItemPickupParticleGroup(this);
        } else if (type == ParticleRenderType.ELDER_GUARDIANS) {
            return new ElderGuardianParticleGroup(this);
        } else {
            return type == ParticleRenderType.NO_RENDER ? new NoRenderParticleGroup(this) : new QuadParticleGroup(this, type);
        }
    }

    protected void updateCount(final ParticleLimit limit, final int change) {
        this.trackedParticleCounts.addTo(limit, change);
    }

    public void extract(final ParticlesRenderState particlesRenderState, final Frustum frustum, final Camera camera, final float partialTickTime) {
        for (ParticleRenderType particleType : RENDER_ORDER) {
            ParticleGroup<?> particles = this.particles.get(particleType);
            if (particles != null && !particles.isEmpty()) {
                particlesRenderState.add(particles.extractRenderState(frustum, camera, partialTickTime));
            }
        }
    }

    public void setLevel(final @Nullable ClientLevel level) {
        this.level = level;
        this.clearParticles();
        this.trackingEmitters.clear();
    }

    public String countParticles() {
        StringBuilder builder = new StringBuilder();
        int total = 0;

        for (Entry<ParticleRenderType, ParticleGroup<?>> group : this.particles.entrySet()) {
            builder.append(group.getKey().shorthand()).append(" ");
            int size = group.getValue().size();
            builder.append(size).append(" ");
            total += size;
        }

        builder.append("T ");
        builder.append(total);
        return builder.toString();
    }

    private boolean hasSpaceInParticleLimit(final ParticleLimit limit) {
        return this.trackedParticleCounts.getInt(limit) < limit.limit();
    }

    public void clearParticles() {
        this.particles.clear();
        this.particlesToAdd.clear();
        this.trackingEmitters.clear();
        this.trackedParticleCounts.clear();
    }

    public RandomSource getRandom() {
        return this.random;
    }
}