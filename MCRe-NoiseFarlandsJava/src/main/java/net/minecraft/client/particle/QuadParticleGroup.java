package net.minecraft.client.particle;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadParticleGroup extends ParticleGroup<SingleQuadParticle> {
    private final ParticleRenderType particleType;
    private final QuadParticleRenderState particleTypeRenderState = new QuadParticleRenderState();

    public QuadParticleGroup(final ParticleEngine engine, final ParticleRenderType particleType) {
        super(engine);
        this.particleType = particleType;
    }

    @Override
    public ParticleGroupRenderState extractRenderState(final Frustum frustum, final Camera camera, final float partialTickTime) {
        for (SingleQuadParticle particle : this.particles) {
            if (frustum.pointInFrustum(particle.x, particle.y, particle.z)) {
                try {
                    particle.extract(this.particleTypeRenderState, camera, partialTickTime);
                } catch (Throwable throwable) {
                    CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                    CrashReportCategory category = report.addCategory("Particle being rendered");
                    category.setDetail("Particle", particle::toString);
                    category.setDetail("Particle Type", this.particleType::toString);
                    throw new ReportedException(report);
                }
            }
        }

        return this.particleTypeRenderState;
    }
}